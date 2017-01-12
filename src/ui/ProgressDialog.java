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
