//Thanks to Sven gothel and the entire JOGL team 
//for providing Oculus support at opengl level.
//For more info: https://github.com/sgothel/jogl

package processing.oculus;

import java.util.Arrays;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLFBODrawable;
import javax.media.opengl.awt.GLCanvas;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.oculusvr.ovrMatrix4f;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.FovHVHalves;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.util.stereo.EyeParameter;
import com.jogamp.opengl.util.stereo.EyePose;
import com.jogamp.opengl.util.stereo.StereoClientRenderer;
import com.jogamp.opengl.util.stereo.StereoDevice;
import com.jogamp.opengl.util.stereo.StereoDeviceFactory;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer;
import com.jogamp.opengl.util.stereo.StereoGLEventListener;

import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;

public class POculus extends PJOGL {

	public POculus(PGraphicsOpenGL pg) {
		super(pg);
	}

	public PMatrix3D oculusProjection;
	public PMatrix3D oculusModelView;
	public float fov;

	@Override
	protected void registerListeners() {
		// Oculus code
		final StereoDeviceFactory stereoDeviceFactory = StereoDeviceFactory
				.createFactory(StereoDeviceFactory.DeviceType.OculusVR);
		if (null == stereoDeviceFactory) {
			System.err.println("No StereoDeviceFactory available");
			return;
		}

		int deviceIndex = 0;
		// connect to device (get context)
		final StereoDevice stereoDevice = stereoDeviceFactory.createDevice(
				deviceIndex, null, true /* verbose */);
		if (null == stereoDevice) {
			System.err.println("No StereoDevice.Context available for index "
					+ deviceIndex);
			return;
		}
		// start sensors
		if (!stereoDevice.startSensors(true)) {
			System.err.println("Could not start sensors on device "
					+ deviceIndex);
		}

		final FovHVHalves[] defaultEyeFov = stereoDevice.getDefaultFOV();

		final float[] eyePositionOffset = stereoDevice.getDefaultEyePositionOffset(); // default
		System.err.println("Eye Position Offset: "
				+ Arrays.toString(eyePositionOffset));
		final int textureUnit = 1;
		final int reqDistortionBits;
		reqDistortionBits = stereoDevice.getRecommendedDistortionBits();
		final float pixelsPerDisplayPixel = 0.75f;
		final StereoDeviceRenderer stereoDeviceRenderer = stereoDevice
				.createRenderer(reqDistortionBits, 2, eyePositionOffset,
						defaultEyeFov, pixelsPerDisplayPixel, textureUnit);
		System.err.println("StereoDeviceRenderer: " + stereoDeviceRenderer);
		final int texFilter = GL.GL_LINEAR;
		final StereoClientRenderer renderer = new StereoClientRenderer(
				stereoDeviceRenderer, true /* ownsDist */, texFilter,
				texFilter, 0);

		if (WINDOW_TOOLKIT == AWT) {
			pg.parent.addListeners(canvasAWT);

			listener = new PGLStereoListener();
			renderer.addGLEventListener((StereoGLEventListener) listener);
			canvasAWT.addGLEventListener(renderer);
		} else if (WINDOW_TOOLKIT == NEWT) {
			if (EVENTS_TOOLKIT == NEWT) {
				NEWTMouseListener mouseListener = new NEWTMouseListener();
				windowNEWT.addMouseListener(mouseListener);
				NEWTKeyListener keyListener = new NEWTKeyListener();
				windowNEWT.addKeyListener(keyListener);
				NEWTWindowListener winListener = new NEWTWindowListener();
				windowNEWT.addWindowListener(winListener);
			} else if (EVENTS_TOOLKIT == AWT) {
				pg.parent.addListeners(canvasNEWT);
			}

			listener = new PGLStereoListener();
			renderer.addGLEventListener((StereoGLEventListener) listener);
			windowNEWT.addGLEventListener(renderer);
		}

		if (canvas != null) {
			canvas.setFocusTraversalKeysEnabled(false);
		}
	}

	protected class PGLStereoListener extends PJOGL.PGLListener implements
			StereoGLEventListener {

		public PGLStereoListener() {
		}

		@Override
		public void display(GLAutoDrawable glDrawable) {
			display(glDrawable, 0);
		}

		@Override
		public void dispose(GLAutoDrawable adrawable) {
		}

		@Override
		public void init(GLAutoDrawable glDrawable) {
			getGL(glDrawable);

			capabilities = glDrawable.getChosenGLCapabilities();
			if (!hasFBOs()) {
				throw new RuntimeException(MISSING_FBO_ERROR);
			}
			if (!hasShaders()) {
				throw new RuntimeException(MISSING_GLSL_ERROR);
			}
			if (USE_JOGL_FBOLAYER && capabilities.isFBO()) {
				int maxs = maxSamples();
				numSamples = PApplet.min(capabilities.getNumSamples(), maxs);
			}
		}

		@Override
		public void reshape(GLAutoDrawable glDrawable, int x, int y, int w,
				int h) {
			// getGL(glDrawable);
		}

		@Override
		public void display(GLAutoDrawable glDrawable, int arg1) {
			getGL(glDrawable);

			if (USE_JOGL_FBOLAYER && capabilities.isFBO()) {
				// The onscreen drawing surface is backed by an FBO layer.
				GLFBODrawable fboDrawable = null;

				if (WINDOW_TOOLKIT == AWT) {
					GLCanvas glCanvas = (GLCanvas) glDrawable;
					fboDrawable = (GLFBODrawable) glCanvas
							.getDelegatedDrawable();
				} else {
					GLWindow glWindow = (GLWindow) glDrawable;
					fboDrawable = (GLFBODrawable) glWindow
							.getDelegatedDrawable();
				}

				if (fboDrawable != null) {
					backFBO = fboDrawable.getFBObject(GL.GL_BACK);
					if (1 < numSamples) {
						if (needSepFrontTex) {
							// When using multisampled FBO, the back buffer is
							// the MSAA
							// surface so it cannot be read from. The sink
							// buffer contains
							// the readable 2D texture.
							// In this case, we create an auxiliary "front"
							// buffer that it is
							// swapped with the sink buffer at the beginning of
							// each frame.
							// In this way, we always have a readable copy of
							// the previous
							// frame in the front texture, while the back is
							// synchronized
							// with the contents of the MSAA back buffer when
							// requested.
							if (frontFBO == null) {
								// init
								frontFBO = new FBObject();
								frontFBO.reset(gl, pg.width, pg.height);
								frontFBO.attachTexture2D(gl, 0, true);
								sinkFBO = backFBO.getSamplingSinkFBO();
								changedFrontTex = changedBackTex = true;
							} else {
								// swap
								FBObject temp = sinkFBO;
								sinkFBO = frontFBO;
								frontFBO = temp;
								backFBO.setSamplingSink(sinkFBO);
								changedFrontTex = changedBackTex = false;
							}
							backTexAttach = (FBObject.TextureAttachment) sinkFBO
									.getColorbuffer(0);
							frontTexAttach = (FBObject.TextureAttachment) frontFBO
									.getColorbuffer(0);
						} else {
							changedFrontTex = changedBackTex = sinkFBO == null;

							// Default setting (to save resources): the front
							// and back
							// textures are the same.
							sinkFBO = backFBO.getSamplingSinkFBO();
							backTexAttach = (FBObject.TextureAttachment) sinkFBO
									.getColorbuffer(0);
							frontTexAttach = backTexAttach;
						}
					} else {
						// w/out multisampling, rendering is done on the back
						// buffer.
						frontFBO = fboDrawable.getFBObject(GL.GL_FRONT);
						backTexAttach = (FBObject.TextureAttachment) backFBO
								.getColorbuffer(0);
						frontTexAttach = (FBObject.TextureAttachment) frontFBO
								.getColorbuffer(0);
					}
				}
			}

			pg.parent.handleDraw();
			drawLatch.countDown();

		}

		private final float[] mat4Tmp1 = new float[16];

		private final float[] mat4Tmp2 = new float[16];

		private final float[] vec3Tmp1 = new float[3];

		private final float[] vec3Tmp2 = new float[3];

		private final float[] vec3Tmp3 = new float[3];

		private final float zNear = pg.cameraNear;

		private final float zFar = pg.cameraFar;

		@Override
		public void reshapeForEye(final GLAutoDrawable drawable, final int x,
				final int y, final int width, final int height,
				final EyeParameter eyeParam, final EyePose eyePose) {
			getGL(drawable);
			// //Projection Matrix
			if (gl2x != null) {
				
				final float[] mat4Projection = FloatUtil.makePerspective(
						mat4Tmp1, 0, true, eyeParam.fovhv, zNear, zFar);
				PMatrix3D proj = new PMatrix3D(mat4Projection[0],
						mat4Projection[4], mat4Projection[8],
						mat4Projection[12], mat4Projection[1],
						mat4Projection[5], mat4Projection[9],
						mat4Projection[13], mat4Projection[2],
						mat4Projection[6], mat4Projection[10],
						mat4Projection[14], mat4Projection[3],
						mat4Projection[7], mat4Projection[11],
						mat4Projection[15]);
				fov = eyeParam.fovhv.vertFov();
				oculusProjection = proj;

				final Quaternion rollPitchYaw = new Quaternion();
				final float[] shiftedEyePos = rollPitchYaw.rotateVector(
						vec3Tmp1, 0, eyePose.position, 0);
				VectorUtil.addVec3(shiftedEyePos, shiftedEyePos,
						eyeParam.positionOffset);
				rollPitchYaw.mult(eyePose.orientation);
				final float[] up = rollPitchYaw.rotateVector(vec3Tmp2, 0,
						VectorUtil.VEC3_UNIT_Y, 0);
				final float[] forward = rollPitchYaw.rotateVector(vec3Tmp3, 0,
						VectorUtil.VEC3_UNIT_Z_NEG, 0);
				final float[] center = VectorUtil.addVec3(forward,
						shiftedEyePos, forward);
				final float[] mLookAt = FloatUtil.makeLookAt(mat4Tmp1, 0,
						shiftedEyePos, 0, center, 0, up, 0, mat4Tmp2);
				final float[] mViewAdjust = FloatUtil.makeTranslation(mat4Tmp2,
						true, eyeParam.distNoseToPupilX,
						eyeParam.distMiddleToPupilY, eyeParam.eyeReliefZ);
				final float[] mat4Modelview = FloatUtil.multMatrix(mViewAdjust,
						mLookAt);

				PMatrix3D model = new PMatrix3D(mat4Modelview[0],
						mat4Modelview[4], mat4Modelview[8], mat4Modelview[12],
						mat4Modelview[1], mat4Modelview[5], mat4Modelview[9],
						mat4Modelview[13], mat4Modelview[2], mat4Modelview[6],
						mat4Modelview[10], mat4Modelview[14], mat4Modelview[3],
						mat4Modelview[7], mat4Modelview[11], mat4Modelview[15]);
				//model.invert();
				oculusModelView = model;
			}

		}

	}

}
