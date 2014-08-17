package processing.oculus.test;

import processing.core.PApplet;
import processing.oculus.Oculus;
import processing.oculus.POculus;
import processing.opengl.PGraphicsOpenGL;

public class HelloOculus extends PApplet {
	public void setup() {
		size(1280, 800, Oculus.RENDERER);
	}

	public void draw() {
		background(100, 100, 100);
		//Change camera to incorporate distortion caused by Oculus
		//TODO: fix in future versions
		beginCamera();
		camera();
		translate(350,800,0);
		rotateX(180);
		endCamera();

		//Set the modelview and projection matrices based on the 
		//sensor data from Oculus
		POculus po = ((POculus) beginPGL());
		PGraphicsOpenGL pgl = (PGraphicsOpenGL) g;
		po.oculusModelView.invert();
		pgl.applyMatrix(po.oculusModelView);
		pgl.setProjection(po.oculusProjection);
		
		
		//Draw actual objects
		box(100);
		translate(100,100,0);
		sphere(100);
	}
		
}
