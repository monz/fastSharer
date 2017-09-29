/*
 * Copyright (c) 2017. Markus Monz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ui;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

public class ProgressDialog extends JDialog {
    private static final Logger log = Logger.getLogger(ProgressDialog.class.getName());

    private String title;
    private String filename;
    private int chunkCount;

    private JProgressBar progressBar;

    public ProgressDialog(String title, String filename, int chunkCount) {
        log.info(String.format("Created progress dialog for file '%s'", filename));

        this.title = title;
        this.filename = filename;
        this.chunkCount = chunkCount;

        initView();
    }

    private void initView() {
        setTitle(title);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel filenameLabel = new JLabel(filename);
        filenameLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panel.add(filenameLabel, BorderLayout.NORTH);

        progressBar = new JProgressBar(0, chunkCount);
        panel.add(progressBar, BorderLayout.CENTER);

        add(panel);

        setMinimumSize(new Dimension(400, 100));
        setLocationRelativeTo(null);

        pack();
    }

    public void update(int finishedChunkCount) {
        progressBar.setValue(finishedChunkCount);
    }
}
