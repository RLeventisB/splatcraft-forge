#version 150

in vec4 vertexColor;
out vec2 texCoord0;

out vec4 fragColor;

void main() {
    fragColor = vec4(0.9, 0.9, 0.9, vertexColor.a);
}