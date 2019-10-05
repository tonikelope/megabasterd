/*
 * Copyright (C) 2019 tonikelope
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.TransferHandler;

/**
 *
 * @author tonikelope
 *
 * Thanks to -> https://stackoverflow.com/users/6286694/abika
 */
class FileDropHandler extends TransferHandler {

    private static final Logger LOG = Logger.getLogger(FileDropHandler.class.getName());

    final FileDropHandlerNotifiable _notifiable;

    FileDropHandler(FileDropHandlerNotifiable notifiable) {
        super();

        _notifiable = notifiable;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        for (DataFlavor flavor : support.getDataFlavors()) {
            if (flavor.isFlavorJavaFileListType()) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!this.canImport(support)) {
            return false;
        }

        List<File> files;

        try {
            files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
        } catch (UnsupportedFlavorException | IOException ex) {
            // should never happen (or JDK is buggy)
            return false;
        }

        THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {
                _notifiable.file_drop_notify(files);
            }
        });

        return true;
    }
}
