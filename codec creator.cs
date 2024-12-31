using System.Runtime.InteropServices;

/*
    hello person that is reading this code!!!!!
    this file is supposed to be executed via a a c# intepreter because i dont want to make a .csproj >:( you can use
    something like https://github.com/waf/CSharpRepl to open this file via the #load directive

    the code will read your clipboard and try to make a codec about it!!! because i cant make it so you can paste the
    code into the console since there are newlines and this is made to take advantage of them

    what you can copy is something like this:

    public record DataRecord(
        int someField,
        boolean anotherField

    if you copy a line that doesn't have the keyword "record" or isn't two words (excluding commas) the file will crash!
    yes! crash because i don't want to handle errors
    so for example copying this entire snippet will crash, since there is a line with only a parentheses:

        public record DataRecord(
            int someField,
            boolean anotherField
        )

    however you can make an "unnamed" codec via copying only the fields, like so:

            int someField,
            boolean anotherField

    this will set the class name as "NotNamed" but will output the codec in the console.
*/

Console.WriteLine("hello we will read ur clipboard and make a codec from it\n");
string[] lines = GetClipboardText().Split(Environment.NewLine).Select(v => v.Replace(",", string.Empty)).ToArray();
if (lines.Length < 2)
    return;
List<Definition> definitions = new List<Definition>();
string recordName = GetName(lines[0]);
for (int i = (recordName == "NotNamed" ? 0 : 1); i < lines.Length; i++)
{
    string line = lines[i];
    string[] words = line.Split(" ");
    if (words.Length == 2)
    {
        string codecName = words[0].Trim();
        definitions.Add(new Definition(codecName, words[1], IsPrimitive(codecName)));
    }
}

Console.WriteLine($@"
public static final Codec<{recordName}> CODEC = RecordCodecBuilder.create(
    inst -> inst.group(");
for (int i = 0; i < definitions.Count; i++)
{
    var definition = definitions[i];
    Console.Write("     " + definition.ToString(recordName));
    if (i < definitions.Count - 1)
    {
        Console.WriteLine(",");
    }
}
Console.WriteLine($@"
    ).apply(inst, {recordName}::new)
);");

bool IsPrimitive(string codec)
{
    switch (codec)
    {
        case "int":
        case "Integer":
        case "double":
        case "Double":
        case "float":
        case "Float":
        case "Boolean":
        case "boolean":
        case "Short":
        case "short":
        case "Byte":
        case "byte":
            return true;
    }
    return false;
}
string GetName(string line)
{
    string[] words = line.Split(" ");
    int index = Array.IndexOf(words, "record");
    if (index == -1)
        return "NotNamed";
    return new string(words[index + 1].Where(v => char.IsLetterOrDigit(v)).ToArray());
}

[DllImport("user32.dll", SetLastError = true)]
static extern bool OpenClipboard(nint hWndNewOwner);

[DllImport("user32.dll", SetLastError = true)]
public static extern IntPtr GetClipboardData(nint uFormat);

[DllImport("user32.dll", SetLastError = true, ExactSpelling = true)]
[return: MarshalAs(UnmanagedType.Bool)]
public static extern bool CloseClipboard();

[DllImport("kernel32.dll", SetLastError = true, ExactSpelling = true)]
public static extern IntPtr GlobalLock(nint hMem);

[DllImport("kernel32.dll", SetLastError = true, ExactSpelling = true)]
[return: MarshalAs(UnmanagedType.Bool)]
public static extern bool GlobalUnlock(nint hMem);

[DllImport("kernel32.dll", SetLastError = true, ExactSpelling = true)]
public static extern nuint GlobalSize([In] nint hMem);

// thanks to whoever made https://www.pinvoke.net/default.aspx/user32.GetClipboardData
// and https://github.com/dahall/Vanara/blob/master/PInvoke/User32/WinUser.Clipboard.cs#L767 for the extensive documentation
// also NO I AM NOT GOING TO STEAL YOUR CREDIT CARD OR SOMETHING its just that i want to literally open this program and make a codec automatically
static string GetClipboardText()
{
    nint handle = nint.Zero;
    OpenClipboard(handle);

    //Get pointer to clipboard data in the selected format
    nint ClipboardDataPointer = GetClipboardData((nint)13);

    //Do a bunch of crap necessary to copy the data from the memory
    //the above pointer points at to a place we can access it.
    nuint Length = GlobalSize(ClipboardDataPointer);
    nint gLock = GlobalLock(ClipboardDataPointer);

    if (Length == 0)
    {
        Console.WriteLine("oops u dont have anything on ur clipboard ok bye");

        GlobalUnlock(gLock); //unlock gLock

        CloseClipboard();

        throw new Exception();
    }
    //Init a buffer which will contain the clipboard data
    byte[] Buffer = new byte[(int)Length];
    //Copy clipboard data to buffer
    Marshal.Copy(gLock, Buffer, 0, (int)Length);

    GlobalUnlock(gLock); //unlock gLock

    CloseClipboard();

    return Encoding.Unicode.GetString(Buffer);
}
static string MakeCamelCase(string text)
{
    StringBuilder builder = new();
    foreach (char chr in text)
    {
        if (char.IsUpper(chr))
        {
            builder.Append('_');
            builder.Append(char.ToLower(chr));
        }
        else
            builder.Append(chr);
    }
    return builder.ToString();
}

struct Definition(string codecName, string fieldName, bool isPrimitive)
{
    public string ToString(string recordName)
    {
        StringBuilder builder = new StringBuilder();
        if (isPrimitive)
        {
            builder.Append("Codec.");
            builder.Append(codecName.ToUpperInvariant());
        }
        else
        {
            builder.Append(codecName);
            builder.Append(".CODEC");
        }
        builder.Append(".fieldOf(\"");
        builder.Append(MakeCamelCase(fieldName));
        builder.Append("\").forGetter(");
        builder.Append(recordName);
        builder.Append("::");
        builder.Append(fieldName);
        builder.Append(")");

        return builder.ToString();
    }
}