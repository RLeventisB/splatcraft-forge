#version 150

uniform sampler2D Sampler0;

uniform vec2 ScreenSize;
uniform mat4 InverseProjMat;

in vec4 vertexColor;

out vec4 fragColor;

// while i know that by inversing the projection matrix you can get the clip space vector,
// i didnt know that opengl normalized them between 0 and 1 >:(
// thanks Andon M. Coleman from https://stackoverflow.com/a/32246825 for doing pretty much 99% of the code here
// however, this method does 12 extra multiplications! :(
float worldDepth(vec2 coord, float depth)
{
    vec4 clipSpacePosition = vec4(coord * 2 - 1, depth * 2 - 1, 1.0);
    vec4 viewSpacePosition = InverseProjMat * clipSpacePosition;

    return depth / viewSpacePosition.w;
}

void main()
{
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;
    float depthToCompare = worldDepth(screenUv, texture(Sampler0, screenUv).r);
    float currentDepth = gl_FragCoord.z / gl_FragCoord.w;

    if (abs(currentDepth - depthToCompare) > 0.1)
    {
        discard;
    }
    fragColor = vertexColor;
}
