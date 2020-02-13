package Methods;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.util.*;

public class DetectMovement implements Runnable {
    static{
        File jarPath=new File(Yolo.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String propertiesPath=jarPath.getParentFile().getParentFile().getParent();
        propertiesPath = propertiesPath.substring(6);
         System.load(propertiesPath+"\\opencv_java411.dll");
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    int restrictionxleft;
    int restrictionxright;
    int restrictionytop;
    int restrictionybottom;
    int how_many;
    int objectsize;
    Mat currentframe;
    Point p1;
    Point p2;
    List<Integer> numberofspaces;
    String address;
    boolean ishorizontal;

    public int getHow_many() {
        return how_many;
    }

    public void setHow_many(int how_many) {
        this.how_many = how_many;
    }

    public Mat getCurrentframe() {
        return currentframe;
    }

    public DetectMovement(int restrictionxleft, int restrictionxright, int restrictionytop, int restrictionybottom, Point p1, Point p2, int objectsize, String address, boolean ishorizontal, int how_many) {
        this.restrictionxleft = restrictionxleft;
        this.restrictionxright = restrictionxright;
        this.restrictionytop = restrictionytop;
        this.restrictionybottom = restrictionybottom;
        numberofspaces = new ArrayList<>();
        this.p1 = p1;
        this.p2 = p2;
        this.objectsize = objectsize;
        this.address = address;
        this.ishorizontal = ishorizontal;
        this.how_many = how_many;
        this.currentframe = new Mat();
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
    public void run() {
        int n = 0;
        List<Point> locations = new ArrayList<>();
        List<Point> tmplocations = new ArrayList<>();
        VideoCapture camera = new VideoCapture(address);
        if (!camera.isOpened()) {
            System.out.println("Error! Camera can't be opened!");
            return;
        }
        int j;
        Mat frame;
        BackgroundSubtractor backSub;
        Mat hierarchy = new Mat();
        int threshold = 600;
        int kernelSize = 11;
        while (true) {
            n++;
            j = 0;
            backSub = Video.createBackgroundSubtractorKNN();
            List<MatOfPoint> contours = new ArrayList<>();
            do {
                frame = new Mat();
                camera.grab();
                if(camera.read(frame)) {
                    currentframe = frame.clone();
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2GRAY);
                    backSub.apply(frame, frame);
                    j++;
                }
            } while (j <= 6);
            Mat element2 = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(1, 1));
            Imgproc.morphologyEx(frame, frame, Imgproc.MORPH_ERODE, element2);
            Imgproc.medianBlur(frame, frame, 3);
            Imgproc.morphologyEx(frame, frame, Imgproc.MORPH_DILATE, element2);
            Imgproc.Canny(frame, frame, threshold, threshold * 2);
            Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(2 * kernelSize + 1, 2 * kernelSize + 1),
                    new Point(kernelSize, kernelSize));
            Imgproc.dilate(frame, frame, element);
            Imgproc.findContours(frame, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
            MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
            Rect[] boundRect = new Rect[contours.size()];
            Point[] centers = new Point[contours.size()];
            float[][] radius = new float[contours.size()][1];
            for (int i = 0; i < contours.size(); i++) {
                contoursPoly[i] = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
                boundRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
                centers[i] = new Point();
                Imgproc.minEnclosingCircle(contoursPoly[i], centers[i], radius[i]);
            }
            List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
            for (MatOfPoint2f poly : contoursPoly) {
                contoursPolyList.add(new MatOfPoint(poly.toArray()));
            }
            for (int i = 0; i < contours.size(); i++) {
                double centerx = (boundRect[i].tl().x + boundRect[i].br().x) / 2;
                double centery = (boundRect[i].tl().y + boundRect[i].br().y) / 2;
                double size = boundRect[i].width * boundRect[i].height;
                if (size > (objectsize/(centery/frame.size().height))) {
                    if (centerx > restrictionxleft && centerx < restrictionxright && centery < restrictionybottom && centery > restrictionytop) {
                        tmplocations.add(new Point(centerx,centery));
                    }
                }
            }
            for (Point lc:locations) {
                Iterator i = tmplocations.iterator();
                Point tmplc;
                while(i.hasNext()) {
                    tmplc = (Point) i.next();
                    if (Math.abs(lc.y - tmplc.y) < 80 && Math.abs(lc.x - tmplc.x) < 80) {
                        if(doIntersect(lc,tmplc,p1,p2)) {
                            double s = (p2.y - p1.y) * lc.x + (p1.x - p2.x) * lc.y + (p2.x * p1.y - p1.x * p2.y);
                            if (s < 0) {
                                how_many--;
                            }
                            else if (s > 0) {
                                how_many++;
                            }
                        }

                    }
                }
            }
            if(this.numberofspaces.size()>10)
            {
                this.numberofspaces.remove(0);
                this.numberofspaces.add(how_many);
            }
            else
            {
                this.numberofspaces.add(how_many);
            }
            locations.clear();
            for (Point tmplc : tmplocations
            ) {
                locations.add(tmplc);
            }
            tmplocations.clear();
        }
    }
    static boolean onSegment(Point p, Point q, Point r)
    {
        if (q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) &&
                q.y <= Math.max(p.y, r.y) && q.y >= Math.min(p.y, r.y))
            return true;

        return false;
    }
    static int orientation(Point p, Point q, Point r)
    {
        double val = (q.y - p.y) * (r.x - q.x) -
                (q.x - p.x) * (r.y - q.y);

        if (val == 0) return 0; // colinear

        return (val > 0)? 1: 2; // clock or counterclock wise
    }
    static boolean doIntersect(Point p1, Point q1, Point p2, Point q2)
    {
        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);

        // General case
        if (o1 != o2 && o3 != o4)
            return true;

        // Special Cases
        // p1, q1 and p2 are colinear and p2 lies on segment p1q1
        if (o1 == 0 && onSegment(p1, p2, q1)) return true;

        // p1, q1 and q2 are colinear and q2 lies on segment p1q1
        if (o2 == 0 && onSegment(p1, q2, q1)) return true;

        // p2, q2 and p1 are colinear and p1 lies on segment p2q2
        if (o3 == 0 && onSegment(p2, p1, q2)) return true;

        // p2, q2 and q1 are colinear and q1 lies on segment p2q2
        if (o4 == 0 && onSegment(p2, q1, q2)) return true;

        return false; // Doesn't fall in any of the above cases
    }
}
