#version 150

// Native 26.2 rendertype_clouds.vsh: face positions are relative to the camera (cell offset added here), the
// distance used for the cloud fade is the spherical distance from the camera, the colour is face shade * cloud colour.

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 CloudOffset;
uniform vec4 CloudColor;

out float vertexDistance;
out vec4 vertexColor;

void main() {
    vec3 pos = Position + CloudOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    vertexDistance = length(pos);
    vertexColor = Color * CloudColor;
}
