double speed = 2.2;
int straightFrames = 2;
double drag = 0.64;
double gravMult = 0.65;
Write("Speed: " + speed);
Write("Straight frames: " + straightFrames);
Write("Drag: " + drag);
Write("Grav Multiplier: " + gravMult);


{
    Write("Splatoon:");
    double pos = 0;
    int frame = 0;
    double currentSpeed = speed;

    while(true)
    {
        bool falling = frame >= straightFrames;
        if(falling)
            currentSpeed *= drag;
        
        if(currentSpeed < 10E-5)
            break;
        Write($"Frame {frame}: pos: {pos}, speed: {currentSpeed * (falling ? gravMult : 1)}");
        
        pos += currentSpeed * (falling ? gravMult : 1);
        frame++;
    }
}
{
    Write("Mining away:");
    double pos = 0;
    int frame = 0;

    while(true)
    {
        double currentSpeed = getSpeed(frame);
        
        if(currentSpeed < 10E-5)
            break;
        Write($"Frame {frame}: pos: {pos}, speed: {currentSpeed}");
        
        pos += currentSpeed;
        frame++;
    }
    
    double getSpeed(int frame)
    {
        double speedLocal = speed;
        float fallenFrames = Math.Max(0, frame - straightFrames);

        if (frame + 1 <= straightFrames)// not close to ending
        {
            return speedLocal;
        }
        else if (frame >= straightFrames) // already ended
        {
            speedLocal *= gravMult;
            speedLocal *= Math.Pow(drag, 1 + fallenFrames);
            return speedLocal;
        }
        float straightFraction = straightFrames - frame;
        float fallFraction = 1 - straightFraction;
        Write(fallFraction);
        return (speedLocal * straightFraction + speedLocal * gravMult * Math.Pow(drag, fallFraction) * fallFraction);
    }
}


void Write(Object obj)
{
    Console.WriteLine(obj);
}