package Methods;

import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC3;

public class Yolo implements Runnable {
    static{
        File jarPath=new File(Yolo.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String propertiesPath=jarPath.getParentFile().getParentFile().getParent();
        propertiesPath = propertiesPath.substring(6);
        System.load(propertiesPath+"\\opencv_java411.dll");
       // System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    int freespaces;
    List<Point> fieldpoints;
    List<Integer> numberofspaces;
    String address;
    Mat currentframe;

    public int getFreespaces() {
        return freespaces;
    }

    public void setFreespaces(int freespaces) {
        this.freespaces = freespaces;
    }
    public Mat getCurrentframe() {
        return currentframe;
    }
    public int freespaces() {
        Integer[] itemsArray = new Integer[this.numberofspaces.size()];
        itemsArray = this.numberofspaces.toArray(itemsArray);
        if(itemsArray.length>0) {
            Arrays.sort(itemsArray);
            double median;
            if (itemsArray.length % 2 == 0)
                median = ((double) itemsArray[itemsArray.length / 2] + (double) itemsArray[itemsArray.length / 2 - 1]) / 2;
            else
                median = (double) itemsArray[itemsArray.length / 2];
            return (int) median;
        }
        return 0;
    }

    private static List<String> getOutputNames(Net net) {
        List<String> names = new ArrayList<>();

        List<Integer> outLayers = net.getUnconnectedOutLayers().toList();
        List<String> layersNames = net.getLayerNames();
        outLayers.forEach((item) -> names.add(layersNames.get(item - 1)));//unfold and create R-CNN layers from the loaded YOLO model//
        return names;
    }
    public Yolo(int freespaces, List<Point> fieldpoints, String address)
    {
        this.freespaces = freespaces;
        this.fieldpoints = fieldpoints;
        numberofspaces = new ArrayList<>();
        this.address = address;
        this.currentframe = new Mat();
    }
    @Override
    public void run() {
        int cars;

        File jarPath=new File(Yolo.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String propertiesPath=jarPath.getParentFile().getParentFile().getParent();
        propertiesPath = propertiesPath.substring(6);
        String modelWeights = propertiesPath+"\\yolov3.weights";
        String modelConfiguration = propertiesPath+"\\yolov3.cfg";
        //String modelWeights = "C:\\Users\\Damian\\IdeaProjects\\CameraParkingRest\\yolov3.weights";
       // String modelConfiguration = "C:\\Users\\Damian\\IdeaProjects\\CameraParkingRest\\yolov3.cfg";

        VideoCapture cap = new VideoCapture(address);
        Mat frame = new Mat();
        Net net = Dnn.readNetFromDarknet(modelConfiguration, modelWeights);
        List<Mat> result = new ArrayList<>();
        List<String> outBlobNames = getOutputNames(net);
            while (cap.read(frame)) {
                currentframe = frame.clone();
                Mat mask = new Mat(frame.size(),CV_8UC3);
                MatOfPoint mop = new MatOfPoint();
                mop.fromList(fieldpoints);
                Imgproc.fillConvexPoly(mask, mop, new Scalar(255, 255, 255));
                cars=0;
                Size sz = new Size(288,288);
                Mat blob = Dnn.blobFromImage(frame, 0.00392, sz, new Scalar(0), true, false);
                net.setInput(blob);
                net.forward(result, outBlobNames);
                float confThreshold = 0.1f;
                for (int i = 0; i < result.size(); ++i)
                {
                    Mat level = result.get(i);
                    for (int j = 0; j < level.rows(); ++j)
                    {
                        Mat row = level.row(j);
                        Mat scores = row.colRange(5, level.cols());
                        Core.MinMaxLocResult mm = Core.minMaxLoc(scores);
                        float confidence = (float)mm.maxVal;
                        Point classIdPoint = mm.maxLoc;
                        if (confidence > confThreshold)
                        {
                            int centerX = (int)(row.get(0,0)[0] * frame.cols());
                            int centerY = (int)(row.get(0,1)[0] * frame.rows());
                            if((int)classIdPoint.x == 2 || (int)classIdPoint.x == 5 || (int)classIdPoint.x == 7   ) {   //check if car,bus or truck
                                if(mask.get(centerY,centerX)[0] == 255.0) {
                                    cars++;
                                }
                            }
                        }
                    }
                }
                if(this.numberofspaces.size()>10)
                {
                    this.numberofspaces.remove(0);
                    this.numberofspaces.add(this.freespaces - cars);
                }
                else
                {
                    this.numberofspaces.add(this.freespaces - cars);
                }
            }
    }
}
