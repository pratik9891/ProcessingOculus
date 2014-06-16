package Trials;
import com.oculusvr.capi.DistortionMesh;
import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.HmdDesc;
import com.oculusvr.capi.OvrLibrary;
import com.oculusvr.capi.OvrLibrary.ovrHmdType;
import com.oculusvr.capi.Posef;
import com.oculusvr.capi.SensorState;

import static com.oculusvr.capi.OvrLibrary.ovrDistortionCaps.*;
import static com.oculusvr.capi.OvrLibrary.ovrHmdType.*;
import static com.oculusvr.capi.OvrLibrary.ovrSensorCaps.*;
import static com.oculusvr.capi.OvrLibrary.ovrEyeType.*;


public class SensorTest {
	public static void main(String[] args){
		Hmd hmd;
		HmdDesc hmdDesc;
	//	OvrLibrary.INSTANCE.ovr_Initialize();
	//	hmd = OvrLibrary.INSTANCE.ovrHmd_CreateDebug(ovrHmd_DK1);
		Hmd.initialize();
		try {
		      Thread.sleep(400);
		    } catch (InterruptedException e) {
		      throw new IllegalStateException(e);
		    }
		hmd = openFirstHmd();
	    if (null == hmd) {
	      throw new IllegalStateException(
	          "Unable to initialize HMD");
	    }
		hmdDesc = hmd.getDesc();
		System.out.println(hmdDesc);
		//DistortionMesh mesh = hmd.createDistortionMesh(ovrEye_Left, hmdDesc.DefaultEyeFov[ovrEye_Left], 0);
		//System.out.println(mesh);
		if (0 == hmd.startSensor(ovrSensorCap_Orientation, 0)) {	
		     throw new IllegalStateException("Unable to start the sensor");
		}
		System.out.println("Sensor started");
		try {
		      Thread.sleep(5000);
		    } catch (InterruptedException e) {
		      throw new IllegalStateException(e);
		    }
	    int i=0;
		while(i<1000){
			SensorState ss = OvrLibrary.INSTANCE.ovrHmd_GetSensorState(hmd, OvrLibrary.INSTANCE.ovr_GetTimeInSeconds());
		//	System.out.println(ss);
			Posef p = ss.Predicted.Pose;
			System.out.println(p.Orientation.x);
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			i++;
		}
		hmd.stopSensor();
	    hmd.destroy();
	    Hmd.shutdown();
	}
	private static Hmd openFirstHmd() {
	    Hmd hmd = Hmd.create(0);
	    if (null == hmd) {
	      hmd = Hmd.createDebug(ovrHmd_DK1);
	    }
	    return hmd;
	  }
}
