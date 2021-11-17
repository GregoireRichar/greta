/*
 * This file is part of Greta.
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
package greta.core.util.speech;

import greta.core.util.time.Temporizable;
import greta.core.util.time.TimeMarker;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Fajrian Yunus
 */


public class SpecialPitchAccent implements Temporizable {
    
    private List<TimeMarker> markers;
    private String id;
    private TimeMarker start;
    private TimeMarker end;
    
    public SpecialPitchAccent(String id, String startSynchPoint, String endSynchPoint){
        this.id = id;
        this.markers = new ArrayList<TimeMarker>();
        this.start = new TimeMarker("start");
        this.start.addReference(startSynchPoint);
        this.markers.add(this.start);
        this.end = new TimeMarker("end");
        this.end.addReference(endSynchPoint);
        this.markers.add(this.end);
    }

    public SpecialPitchAccent(String id, String startSynchPoint){
        //assuming the duration is 1 (following the default duration of Hstar in PitchAccent
        this(id, startSynchPoint, id+":start + "+1);
    }

    @Override
    public List<TimeMarker> getTimeMarkers() {
        return this.markers;
    }

    @Override
    public TimeMarker getTimeMarker(String name) {
        if(name.equalsIgnoreCase("start")) {
            return this.markers.get(0);
        }
        if(name.equalsIgnoreCase("end")) {
            return this.markers.get(1);
        }
        return null;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void schedule() {
        TimeMarker start = this.markers.get(0);
        TimeMarker end = this.markers.get(1);
        if(! start.isConcretized()){
            if(end.isConcretized()) {
                start.setValue(end.getValue()-1);
            }
            else {
                start.setValue(0);
            }
        }
        if(! end.isConcretized()) {
            end.setValue(start.getValue()+1);
        }
    }

    @Override
    public TimeMarker getStart() {
        return this.start;
    }

    @Override
    public TimeMarker getEnd() {
        return this.end;
    }
    
}
