import processing.oculus.*;

void setup() {
  size(1280,800,Oculus.P3D);
}

void draw() {
  background(100, 100, 150);
  POculus po = ((POculus)beginPGL());
  PGraphicsOpenGL pgl = (PGraphicsOpenGL) g;
  pgl.applyMatrix(po.oculusProjection);
  pgl.setProjection(po.oculusModelView);
  pushMatrix(); 
  translate(150, 150, 0);
  rotateY(0.5f);
  rotateZ(0.3f);
  box(100);
  popMatrix();
  translate(250, 210, -100);
  sphere(100);
}