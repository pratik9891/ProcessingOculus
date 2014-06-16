package Trials;

import Access.Oculus;
import JMonkeyMath.Quaternion;

public class TestOrientation {
	public static void main(String[] args) {
		Oculus.initialize();
		Oculus.getHMDInfo();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Quaternion t = Oculus.getOrientation();
		System.out.println(t.toString());
		Oculus.destroy();
	}
}
