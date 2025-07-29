#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV2;

uniform sampler2D Sampler0;

uniform mat4 ModelViewMat;
uniform mat4 OrthoProjMat;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    gl_Position = OrthoProjMat * ModelViewMat * vec4(Position, 1.0);
    //    gl_Position.x *= sqrt(gl_Position.z);
    //    gl_Position.y *= sqrt(gl_Position.z);
    //    gl_Position.z = 1;
    //    gl_Position.w = 1;

    texCoord0 = UV0;
    vertexColor = Color * texelFetch(Sampler0, UV2 / 16, 0);
}
