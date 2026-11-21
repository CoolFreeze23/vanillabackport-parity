#version 150

// Native 26.2 rendertype_clouds.fsh: alpha fades linearly from the camera to FogCloudsEnd.

uniform float CloudEnd;

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = vertexColor;
    float fogValue = vertexDistance <= 0.0 ? 0.0 : (vertexDistance >= CloudEnd ? 1.0 : vertexDistance / CloudEnd);
    color.a *= 1.0 - fogValue;
    fragColor = color;
}
