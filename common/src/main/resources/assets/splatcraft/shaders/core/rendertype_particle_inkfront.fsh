#version 150

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 texelColor = texture(Sampler0, texCoord0);
    if (texelColor.a < 0.1) {
        discard;
    }
    float brightness = texelColor.r;
    vec3 rgb = mix(vertexColor.rgb, vec3(1f, 1f, 1f), brightness * brightness);
    fragColor = vec4(rgb, texelColor.a * vertexColor.a);
}