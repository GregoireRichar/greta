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
package greta.core.animation.mpeg4.bap.file;

import greta.core.animation.mpeg4.bap.BAPFrame;
import greta.core.animation.mpeg4.bap.BAPFramesEmitterImpl;
import greta.core.animation.mpeg4.bap.BAPParser;
import greta.core.util.id.ID;
import greta.core.util.id.IDProvider;
import java.io.File;
import java.util.List;

/**
 *
 * @author Andre-Marie Pez
 */
public class BAPFileReader extends BAPFramesEmitterImpl{

    private BAPParser parser = new BAPParser();

    public void load(String bapfilename) {
        List<BAPFrame> bap_animation = parser.readFromFile(bapfilename, true);

        if (!bap_animation.isEmpty()) {
            String base = (new File(bapfilename)).getName().replaceAll("\\.bap$", "");
            ID id = IDProvider.createID(base);

            //send to all BAPPerformer added
            sendBAPFrames(id, bap_animation);
        }
    }

    public java.io.FileFilter getFileFilter() {
        return new java.io.FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().toLowerCase().endsWith(".bap");
            }
        };
    }

}
