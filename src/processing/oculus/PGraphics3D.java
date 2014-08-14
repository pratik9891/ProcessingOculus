package processing.oculus;

import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

public class PGraphics3D extends processing.opengl.PGraphics3D {
	protected PGL createPGL(PGraphicsOpenGL pg) {
	    return new POculus(pg);
	  }
}
