import processing.oculus.*;

void setup() {
  size(1280,800);
}

void draw() {
  background(100);
  
  //Change camera to incorporate distortion caused by Oculus
  //TODO: fix in future versions (library should handle this)
  beginCamera();
  camera();
  translate(350,800,0);
  rotateX(180);
  endCamera();
  
  //Set the modelview and projection matrices based on the 
  //sensor data from Oculus
  POculus po = ((POculus) beginPGL());
  PGraphicsOpenGL pgl = (PGraphicsOpenGL) g;
  po.oculusModelView.invert(); //depends on the orientation, might need to comment this out!
  pgl.applyMatrix(po.oculusModelView);
  pgl.setProjection(po.oculusProjection);
  
  //Draw the scene
  sphere(100);
  
}