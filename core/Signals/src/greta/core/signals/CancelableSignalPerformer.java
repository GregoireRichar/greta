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
package greta.core.signals;

import greta.core.util.id.ID;

/**
 * This interface describes a {@code SignalPerformer} which's signals are cancelable.
 *
 * @author Nawhal Sayarh
 */
public interface CancelableSignalPerformer extends SignalPerformer {
    /**
     * Cancels all the {@code Signal} with the given {@code ID} if possible.
     * @param requestId ID of the signals to cancel
     */
    void cancelSignalsById (ID requestId);
}
