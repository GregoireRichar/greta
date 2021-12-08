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

import com.illposed.osc.OSCBadDataEvent;
import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacketEvent;
import com.illposed.osc.OSCPacketListener;
import greta.auxiliary.openface2.util.OpenFaceFrame;
import greta.core.util.time.Timer;
import java.util.logging.Logger;
import com.illposed.osc.transport.udp.OSCPortIn;
import com.illposed.osc.OSCPacket;
import greta.core.util.math.Vec3d;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 * This file represents an OpenFace2 frame
 *
 * @author Philippe Gauthier <philippe.gauthier@sorbonne-universite.fr>
 * @author Brice Donval
 */
public class OpenFaceOutputStreamOSCReader extends OpenFaceOutputStreamAbstractReader implements OSCPacketListener{
    protected static final Logger LOGGER = Logger.getLogger(OpenFaceOutputStreamOSCReader.class.getName());
    public static final int DEFAULT_OSC_PORT = 10000;
    private int port = DEFAULT_OSC_PORT;

    private OSCPortIn oscPortIn;

    private boolean isConnected = false;	
    
    private Vec3d gaze0=new Vec3d();
    private Vec3d gaze1=new Vec3d();
    private Vec3d headPoseT=new Vec3d();
    private Vec3d headPoseR=new Vec3d();

    private int headerCount=0;
    /* ---------------------------------------------------------------------- */

    public OpenFaceOutputStreamOSCReader() {       
        
    }

    /* ---------------------------------------------------------------------- */

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    /* ---------------------------------------------------------------------- */


    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        stopConnection();
        this.port = port;
    }

    public String getURL() {
        return "osc://:" + port;
    }

    /* ---------------------------------------------------------------------- */

    public void startConnection() {
        stopConnection(); 
        try { 
            resetHeader();
            oscPortIn = new OSCPortIn(port);
            oscPortIn.addPacketListener(this);
            /*oscPortIn.setResilient(true);
            oscPortIn.setDaemonListener(false);*/
            oscPortIn.startListening();
            isConnected = true;
            startThread();
        }
        catch (IOException ex)
        {
            isConnected = false;
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage());
        }
        if (isConnected) {            
            LOGGER.info(String.format("Connected to: %s", getURL()));            
        } else {
            LOGGER.warning(String.format("Failed to open: %s", getURL()));
            stopConnection();
        }
    }

    public void stopConnection() {
        isConnected = false;
        Timer.sleep(50);
        stopThread();
        if (oscPortIn != null) {
            
            oscPortIn.stopListening();
            oscPortIn = null;
        }        
    }

    /* ---------------------------------------------------------------------- */

    @Override
    public void run() {
        LOGGER.info(String.format("Thread: %s running", OpenFaceOutputStreamOSCReader.class.getName()));
        try {
            LOGGER.fine(String.format("Thread: %s", OpenFaceOutputStreamOSCReader.class.getName()));
            while (true) {
                while (isConnected) {
                    curFrame.gaze0 = gaze0;                            
                    curFrame.gaze1 = gaze1;
                    curFrame.headPoseT = headPoseT;
                    curFrame.headPoseR = headPoseR;
                                
                    
                    postProcessFrame();
                    //LOGGER.info(String.format("run: %s",curFrame));
                    Thread.sleep(10);
                    preProcessFrame();
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException ex) {
            LOGGER.warning(String.format("Thread: %s interrupted", OpenFaceOutputStreamOSCReader.class.getName()));
        }
        cleanHeader();
        LOGGER.info(String.format("Thread: %s exiting", OpenFaceOutputStreamOSCReader.class.getName()));
    }

    /* ---------------------------------------------------------------------- */

    @Override
    public void finalize() throws Throwable {
        stopConnection();
        super.finalize();
    }
    
    private void processOSCMessage(OSCMessage msg){
        List<Object> arguments = msg.getArguments();
        String address = msg.getAddress();
        if(address.indexOf("/of/")==0 && arguments.size()>0){            
           String key = address.substring(4);
           addHeader(key);
           Object arg = arguments.get(0);
           
           double f= 0.0;
           if(arg instanceof Float)
               f = ((Float)arg).doubleValue();
           else if(arg instanceof Double)
               f = ((Double)arg);
                      
           if("frame".equals(key)){
               if(arg instanceof Float) curFrame.frameNumber = ((Float)arg).intValue();
               if(arg instanceof Integer)curFrame.frameNumber = (Integer)arg;
           }               
           else if("face_id".equals(key))
               curFrame.faceId = (Integer)arg;
           else if("timestamp".equals(key))
               curFrame.timestamp = f;
           else if("confidence".equals(key))
               curFrame.confidence = f;
           else if("success".equals(key))
               curFrame.success = (Boolean)arg;
           else if("gaze_0_x".equals(key))
               gaze0.setX(f);
           else if("gaze_0_y".equals(key))
               gaze0.setY(f);
           else if("gaze_0_z".equals(key))
               gaze0.setZ(f);
           else if("gaze_1_x".equals(key))
               gaze1.setX(f);
           else if("gaze_1_y".equals(key))
               gaze1.setY(f);
           else if("gaze_1_z".equals(key))
               gaze1.setZ(f);
           else if("gaze_angle_x".equals(key))
               curFrame.gazeAngleX = f;
           else if("gaze_angle_y".equals(key))
               curFrame.gazeAngleY = f;
           else if("pose_Tx".equals(key))
               headPoseT.setX(f);
           else if("pose_Ty".equals(key))
               headPoseT.setY(f);
           else if("pose_Tz".equals(key))
               headPoseT.setZ(f);
           else if("pose_Rx".equals(key))
               headPoseR.setX(f);
           else if("pose_Ry".equals(key))
               headPoseR.setY(f);
           else if("pose_Rz".equals(key))
               headPoseR.setZ(f);
           else{
               curFrame.setAuFromKey(key,f);               
           }
        }
    }

    @Override
    public void handlePacket(OSCPacketEvent oscpe) {
        OSCPacket p = oscpe.getPacket();
        if(p instanceof OSCMessage)
            processOSCMessage((OSCMessage)p);
        else if(p instanceof OSCBundle){
            OSCBundle bundle = (OSCBundle)p;
       
            for(OSCPacket packet : bundle.getPackets())
            {
                if(packet instanceof OSCMessage)
                    processOSCMessage((OSCMessage)packet);
            }
        }     
    }

    @Override
    public void handleBadData(OSCBadDataEvent oscbde) {
        LOGGER.info(String.format("handleBadData: %s", oscbde.getException().getMessage()));
    }

    private void addHeader(String key) {
        if(!OpenFaceFrame.hasHeader(key)){
            OpenFaceFrame.addHeader(key,headerCount++);
            OpenFaceFrame.availableFeatures.add(key);
            headerChanged(OpenFaceFrame.availableFeatures);
            //LOGGER.info(String.format("Header added: %s",key));
        }
    }
    
    private void resetHeader(){
        headerCount=0;
        OpenFaceFrame.resetHeader();
    }
}
