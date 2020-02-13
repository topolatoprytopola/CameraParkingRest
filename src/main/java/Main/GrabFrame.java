package Main;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.io.File;

public class GrabFrame implements Runnable {
    static{
        File jarPath=new File(GrabFrame.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String propertiesPath=jarPath.getParentFile().getParentFile().getParent();
        propertiesPath = propertiesPath.substring(6);
        System.load(propertiesPath+"\\opencv_java411.dll");
        // System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    Mat currentframe;
    VideoCapture camera;
    Mat frame;
    public Mat getCurrentframe() {
        return currentframe;
    }
    public GrabFrame(String address) {
        this.currentframe = new Mat();
        this.camera = new VideoCapture(address);
        if (!camera.isOpened()) {
            System.out.println("Error! Camera can't be opened!");
            return;
        }
        this.frame = new Mat();
    }
    @Override
    public void run() {
        camera.read(frame);
        if (!frame.empty()) {
            System.out.println(frame);
            this.currentframe = frame;
        }

    }
}
