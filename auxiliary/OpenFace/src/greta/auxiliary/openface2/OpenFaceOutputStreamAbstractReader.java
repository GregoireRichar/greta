/*
 * This file is part of the auxiliaries of Greta.
 *
 * Greta is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Greta is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Greta.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package greta.auxiliary.openface2;

import greta.auxiliary.openface2.util.OpenFaceFrame;
import greta.auxiliary.openface2.util.StringArrayListener;
import greta.auxiliary.openface2.util.ConnectionListener;
import greta.core.animation.mpeg4.bap.BAPFrame;
import greta.core.animation.mpeg4.bap.BAPType;
import greta.core.repositories.AUAPFrame;
import greta.core.util.Constants;
import greta.core.util.id.ID;
import greta.core.util.id.IDProvider;
import greta.core.util.time.Timer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import com.illposed.osc.*;
import com.illposed.osc.transport.udp.OSCPortOut;
import greta.auxiliary.openface2.util.ArrayOfDoubleFilterPow;
import greta.core.animation.mpeg4.bap.BAPFrameEmitter;
import greta.core.animation.mpeg4.bap.BAPFrameEmitterImpl;
import greta.core.animation.mpeg4.bap.BAPFramePerformer;
import greta.core.keyframes.face.AUEmitter;
import greta.core.keyframes.face.AUEmitterImpl;
import greta.core.keyframes.face.AUPerformer;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Brice Donval
 */
public abstract class OpenFaceOutputStreamAbstractReader implements Runnable, AUEmitter, BAPFrameEmitter {
    protected static final Logger LOGGER = Logger.getLogger(OpenFaceOutputStreamAbstractReader.class.getName());

    protected AUEmitterImpl auEmitter = new AUEmitterImpl();
    protected BAPFrameEmitterImpl bapFrameEmitter = new BAPFrameEmitterImpl();

    /* ---------------------------------------------------------------------- */

    protected Thread thread;
    protected final String threadName = OpenFaceOutputStreamAbstractReader.class.getSimpleName();

    protected List<ConnectionListener> connectionListeners = new ArrayList<>();
    protected List<StringArrayListener> headerListeners = new ArrayList<>();

    protected String[] selectedFeatures;

    /* ---------------------------------------------------------------------- */

    private int startInputFrame = 0;
    private final int offsetFrame = 0;
    protected int curGretaFrame;

    protected double fps = 0.0;
    protected double frameDuration = 0.0;
    protected OpenFaceFrame curFrame = new OpenFaceFrame();
    protected OpenFaceFrame prevFrame = new OpenFaceFrame();
    protected ArrayOfDoubleFilterPow filterAUs = new ArrayOfDoubleFilterPow(64,5,.5);
    protected ArrayOfDoubleFilterPow filterBAP = new ArrayOfDoubleFilterPow(3,5,.5);
    protected boolean useFilter = true;
    protected OSCPortOut oscOut = null;

    protected boolean isPerforming = false;
    // loop variables
    protected double prev_rot_X = 0.0;
    protected double prev_rot_Y = 0.0;
    protected double prev_rot_Z = 0.0;

    protected double min_time = Double.MAX_VALUE;
    protected double max_time = 0.0;				

    protected double alpha = 0.75; //1.0;
    
    
    protected OpenFaceOutputStreamAbstractReader() {
        
    }
    
    public void setOSCout(OSCPortOut oscPort){
        this.oscOut = oscPort;
    }
    
    public int getFilterMaxQueueSize(){
        return filterAUs.getMaxSizePerQueue();
    }
    
    public void setFilterMaxQueueSize(int i){
        LOGGER.info(String.format("setFilterMaxQueueSize: %d",i));
        filterAUs.setMaxSizePerQueue(i);
        filterBAP.setMaxSizePerQueue(i);
    }
    
    public double getFilterPow(){
        return filterAUs.getPow();
    }
    
    public void setFilterPow(double d){
        LOGGER.info(String.format("setFilterPow: %f",d));        
        filterAUs.setPow(d);
        filterBAP.setPow(d);
    }

    public boolean isPerforming() {
        return isPerforming;
    }
    
    public void setPerforming(boolean on) {
        isPerforming = on;
    }

    /* ---------------------------------------------------------------------- */
    protected abstract Logger getLogger();

    /* ---------------------------------------------------------------------- */
    protected void startThread() {
        if (thread == null) {
            getLogger().fine(String.format("Starting %s..", threadName));
            thread = new Thread(this, threadName);
            thread.start();
            fireConnection();
        }
    }

    protected void stopThread() {
        if (thread != null) {
            getLogger().fine(String.format("Stopping %s..", threadName));
            thread.interrupt();
            thread = null;
            fireDisconnection();
        }
    }

    protected void cleanHeader() {
        OpenFaceFrame.readHeader(null);
        headerChanged(OpenFaceFrame.availableFeatures);
    }

    /* ---------------------------------------------------------------------- */
    protected void preProcessFrame() {
        if (isPerforming() ) {
            curGretaFrame = (int) (Timer.getTime() * Constants.FRAME_PER_SECOND);
            curFrame.frameNumber = curGretaFrame;
            //prevFrame.copy(curFrame);

            if (startInputFrame == 0) {
                startInputFrame = curFrame.frameNumber;
            }
        }
    }
    
    protected void postProcessFrame(){
        if (isPerforming() ) {
            //curFrame.frameNumber += offsetFrame-startInputFrame + curGretaFrame;
            //curFrame.frameNumber = offsetFrame + curGretaFrame;
            int frameDiff = curFrame.frameNumber - prevFrame.frameNumber;
            if (0 <=frameDiff && frameDiff < 10) { // If less than 10 frame delay
                frameDuration = curFrame.timestamp - prevFrame.timestamp;
                fps = 1.0 / frameDuration;
                //LOGGER.fine(String.format("frameNumber: %d, fps:%f, f dur:%f",curFrame.frameNumber, fps, frameDuration));
                sendFAB_BAP();
            }
            //else LOGGER.info(String.format("Skipping sendFAB_BAP, framediff: %d",frameDiff));
            prevFrame.copy(curFrame);
        }
    }

    protected void sendFAB_BAP() {
        // Format based on <https://github.com/TadasBaltrusaitis/OpenFace>:
        // frame,           face_id,        timestamp,      confidence,     success,
        // gaze_0_x,        gaze_0_y,       gaze_0_z,
        // gaze_1_x,        gaze_1_y,       gaze_1_z,
        // gaze_angle_x,    gaze_angle_y,
        // pose_Tx,         pose_Ty,        pose_Tz,
        // pose_Rx,         pose_Ry,        pose_Rz
        // AUs_r,           AUs_c

        if (isPerforming()) {
            if (frameDuration != 0) {
                if (frameDuration > max_time) {
                    max_time = frameDuration;
                }
                if (frameDuration < min_time) {
                    min_time = frameDuration;
                }              
                sendAUFrame(makeAUFrame());
                sendBAPFrame(makeBAPFrame());
            }
        }
    }

    private AUAPFrame makeAUFrame() {
        AUAPFrame au_frame = new AUAPFrame();
        au_frame.setFrameNumber(curFrame.frameNumber);
        Map<String, Double> values = new TreeMap<>();
        Map<String, Double> masks = new TreeMap<>();
        Map<String, Double> valuesFiltered = new TreeMap<>();
        for (int i = 0; i < OpenFaceFrame.getAUFeatureMasksCount(); ++i) {
            // we assume both tables have corresponding values. AU**_c acts as a mask
            if (Double.isNaN(curFrame.aus[i]) || Double.isInfinite(curFrame.aus[i]))
                curFrame.aus[i] = 0.;
            //if (!Double.isNaN(curFrame.aus[i]) && !Double.isInfinite(curFrame.aus[i])) {
            double value = curFrame.aus[i]*curFrame.auMasks[i]; // non linear curve to get to 1.
            values.put(OpenFaceFrame.getAUFeatureKey(i), value);
            masks.put(OpenFaceFrame.getAUFeatureKey(i), curFrame.auMasks[i]);					  
            double intensity;  
            if(isUseFilter()){
                intensity = filterAUs.pushAndGetFiltered(i, value);                    
                valuesFiltered.put(OpenFaceFrame.getAUFeatureKey(i),intensity);
            }
            else{
                double prevValue = prevFrame.intensity[i];
                intensity = alpha * value + (1 - alpha) * prevValue; // filter																										 
            }
            au_frame.setAUAPboth(OpenFaceFrame.getAUFeatureMaskNumber(i), intensity);
        }  
        //LOGGER.info(String.format("curFrame: %s",curFrame));
        // gaze
        double gaze_x = 0.5 * (curFrame.gaze0.x() + curFrame.gaze1.x());
        double gaze_y = 0.5 * (curFrame.gaze0.y() + curFrame.gaze1.y());
        values.put("gaze_x",gaze_x);
        values.put("gaze_y",gaze_y);
        if(isUseFilter()){
            gaze_x = filterAUs.pushAndGetFiltered(61, gaze_x);        
            gaze_y = filterAUs.pushAndGetFiltered(63, gaze_y);
            valuesFiltered.put("gaze_x",gaze_x);
            valuesFiltered.put("gaze_y",gaze_y);
        }
        
        if (gaze_x < 0) {
            au_frame.setAUAPboth(62, -gaze_x);
        } else {
            au_frame.setAUAPboth(61, gaze_x);
        }

        if (gaze_y < 0) {
            au_frame.setAUAPboth(64, -gaze_y);
        } else {
            au_frame.setAUAPboth(63, gaze_y);
        }
        
        // Send to OSC to debug signal        
        sendOSC("/au_raw/", values);
        sendOSC("/au_mask/", masks);
        if(isUseFilter()){
            sendOSC("/au_filtered/", valuesFiltered);
        }           
        
        // blink						  
        au_frame.setAUAPboth(43, curFrame.blink);
        return au_frame;
    }
    
    private void sendOSC(String root, Map<String,Double> map){
        try {
            for (String key : map.keySet()) {
                final List<Double> args = new ArrayList<>();
                args.add(map.get(key));
                OSCMessage msg = new OSCMessage(root+key, args);  
                if(oscOut!=null)
                    oscOut.send(msg);            
            }
        } catch (OSCSerializeException | IOException ex) {
            LOGGER.warning(ex.getLocalizedMessage());
        } 
    }

    private BAPFrame makeBAPFrame() {
        BAPFrame hmFrame = new BAPFrame();
        hmFrame.setFrameNumber(curFrame.frameNumber);

        if(!curFrame.isNull){
            double rot_X_rad = curFrame.headPoseR.x();
            double rot_Y_rad = -1.0 * curFrame.headPoseR.y();
            double rot_Z_rad = -1.0 * curFrame.headPoseR.z();

            double rot_X_deg = rot_X_rad * 180 / Math.PI;
            double rot_Y_deg = rot_Y_rad * 180 / Math.PI;
            double rot_Z_deg = rot_Z_rad * 180 / Math.PI;

            if(useFilter){
                rot_X_deg = filterBAP.pushAndGetFiltered(0, rot_X_deg);
                rot_Y_deg = filterBAP.pushAndGetFiltered(1, rot_Y_deg);
                rot_Z_deg = filterBAP.pushAndGetFiltered(2, rot_Z_deg);
            }
            else{
                rot_X_deg = alpha * (rot_X_deg) + (1 - alpha) * prev_rot_X;
                rot_Y_deg = alpha * (rot_Y_deg) + (1 - alpha) * prev_rot_Y;
                rot_Z_deg = alpha * (rot_Z_deg) + (1 - alpha) * prev_rot_Z;

                prev_rot_X = rot_X_deg;
                prev_rot_Y = rot_Y_deg;
                prev_rot_Z = rot_Z_deg;
            }

            hmFrame.setDegreeValue(BAPType.vc3_tilt, rot_X_deg);
            hmFrame.setDegreeValue(BAPType.vc3_torsion, rot_Y_deg);
            hmFrame.setDegreeValue(BAPType.vc3_roll, rot_Z_deg);
        }
        return hmFrame;
    }

    /* ---------------------------------------------------------------------- */

    public void addConnectionListener(ConnectionListener connectionListener) {
        if (connectionListener != null && !connectionListeners.contains(connectionListener)) {
            connectionListeners.add(connectionListener);
        }
    }

    public void removeConnectionListener(ConnectionListener connectionListener) {
        if (connectionListener != null && connectionListeners.contains(connectionListener)) {
            connectionListeners.remove(connectionListener);
        }
    }

    private void fireConnection() {
        for (ConnectionListener connectionListener : connectionListeners) {
            connectionListener.onConnection();
        }
    }

    private void fireDisconnection() {
        for (ConnectionListener connectionListener : connectionListeners) {
            connectionListener.onDisconnection();
        }
    }

    /* ---------------------------------------------------------------------- */

    public void addHeaderListener(StringArrayListener headerListener) {
        if (headerListener != null && !headerListeners.contains(headerListener)) {
            headerListeners.add(headerListener);
        }
    }

    public void removeHeaderListener(StringArrayListener headerListener) {
        if (headerListener != null && headerListeners.contains(headerListener)) {
            headerListeners.remove(headerListener);
        }
    }

    protected void headerChanged(List<String> newFeatures) {
        headerListeners.forEach((headerListener) -> {
            headerListener.stringArrayChanged(newFeatures);
        });
    }

    /* ---------------------------------------------------------------------- */

    /**
     * Set selected features
     *
     * @param selected features to use
     */
    public void setSelected(String[] selected) {
        if (selected != null) {
            if (!Arrays.equals(selected, selectedFeatures)) {
                selectedFeatures = selected;
                OpenFaceFrame.setSelectedFeatures(selectedFeatures);
            }
            getLogger().info(String.format("Setting selected features to: %s", Arrays.toString(selected)));
        } else {
            getLogger().warning("No header selected");
        }
    }

    /* ---------------------------------------------------------------------- */

    protected void sendAUFrame(AUAPFrame auFrame) {
        ID id = IDProvider.createID(threadName + "_sendAUFrame");
        auEmitter.performAUAPFrame(auFrame, id);
    }
    

    protected void sendBAPFrame(BAPFrame bapFrame) {
        ID id = IDProvider.createID(threadName + "_sendBAPFrame");
        bapFrameEmitter.sendBAPFrame(id, bapFrame);
    }

    /* ---------------------------------------------------------------------- */

    @Override
    public void finalize() throws Throwable {
        stopThread();;
        super.finalize();
    }

    /**
     * @return the useFilter
     */
    public boolean isUseFilter() {
        return useFilter;
    }

    /**
     * @param useFilter the useFilter to set
     */
    public void setUseFilter(boolean useFilter) {
        getLogger().info("setUseFilter: "+useFilter);
        this.useFilter = useFilter;
        if(useFilter){
            filterAUs.clear();
            filterBAP.clear();
        }
    }
    
    /* ---------------------------------------------------------------------- *
     *                               AUEmitter                                *
     * ---------------------------------------------------------------------- */

    @Override
    public void addAUPerformer(AUPerformer auPerformer) {
        if (auPerformer != null) {
            LOGGER.fine("addAUPerformer");
            auEmitter.addAUPerformer(auPerformer);
        }
    }

    @Override
    public void removeAUPerformer(AUPerformer auPerformer) {
        if (auPerformer != null) {
            LOGGER.fine("removeAUPerformer");
            auEmitter.removeAUPerformer(auPerformer);
        }
    }

    public void sendAUFrame(AUAPFrame auFrame, ID id) {
        //LOGGER.info("sendAUFrame");
        auEmitter.performAUAPFrame(auFrame, id);
    }

    /* ---------------------------------------------------------------------- *
     *                            BAPFrameEmitter                             *
     * ---------------------------------------------------------------------- */

    @Override
    public void addBAPFramePerformer(BAPFramePerformer bapFramePerformer) {
        if (bapFramePerformer != null) {
            LOGGER.fine("addBAPFramePerformer");
            bapFrameEmitter.addBAPFramePerformer(bapFramePerformer);
        }
    }

    @Override
    public void removeBAPFramePerformer(BAPFramePerformer bapFramePerformer) {
        if (bapFramePerformer != null) {
            LOGGER.fine("removeBAPFramePerformer");
            bapFrameEmitter.removeBAPFramePerformer(bapFramePerformer);
        }
    }

    public void sendBAPFrame(BAPFrame bapFrame, ID id) {
        //LOGGER.info("sendBAPFrame");
        bapFrameEmitter.sendBAPFrame(id, bapFrame);
    }
}
