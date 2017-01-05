package ui;

import ui.controller.OverviewController;
import ui.logic.DroppedFileListener;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Overview extends JFrame {
    private static final OverviewController SHARER_CONTROLLER = OverviewController.getInstance();
    private JMenuBar menuBar;

    private static final long serialVersionUID = 1L;

    public Overview(String title) {
        this.setTitle(title);
        initMenu();
        init();
    }

    private void init() {
        this.setMinimumSize(new Dimension(800, 600));
        this.setLocationRelativeTo(null); // center window
        this.setJMenuBar(menuBar);

        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        ((JPanel) this.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel tagSharerId = new JLabel("SharerId: ");

        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT));
        info.add(tagSharerId);
        info.add(defineInfo());

        JPanel main = new JPanel(new GridLayout(2, 2));
        main.add(defineDropArea());
        main.add(defineNodeList());
        main.add(defineSharedFileList());

        this.setLayout(new BorderLayout());

        this.add(info, BorderLayout.NORTH);
        this.add(main, BorderLayout.CENTER);

        this.pack();
    }

    private JTextField defineInfo() {
        JTextField sharerId = new JTextField();
        sharerId.setEditable(false);
        sharerId.setDocument(SHARER_CONTROLLER.getIdModel());

        return sharerId;
    }

    private JPanel defineDropArea() {
        JPanel p = new JPanel();

        p.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JTextField description = new JTextField("Drop files here.");
        description.setEditable(false);
        description.setBackground(null);
        description.setBorder(null);

        p.setDropTarget(new DropTarget(p, new DroppedFileListener()));

        p.add(description);
        return p;
    }

    private JPanel defineNodeList() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        JLabel description = new JLabel("Nodes"); // temporary
        description.setHorizontalAlignment(SwingConstants.CENTER);

        JList<String> nodes = new JList<>(SHARER_CONTROLLER.getNodeListModel());
        JPanel nodePanel = new JPanel(new BorderLayout());
        nodePanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        nodePanel.add(nodes, BorderLayout.CENTER);

        p.add(description, BorderLayout.NORTH);
        p.add(nodePanel, BorderLayout.CENTER);

        return p;
    }

    private JPanel defineSharedFileList() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        JLabel description = new JLabel("Shared files"); // temporary
        description.setHorizontalAlignment(SwingConstants.CENTER);

        JList<String> files = new JList<>(SHARER_CONTROLLER.getFileListModel());
        JPanel filesPanel = new JPanel(new BorderLayout());
        filesPanel.add(files, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(files);

        p.add(description, BorderLayout.NORTH);
        p.add(scrollPane, BorderLayout.CENTER);

        return p;
    }

    private void initMenu() {
        menuBar = new JMenuBar();
        menuBar.add(getFileMenu());
        menuBar.add(getEditMenu());
    }

    private JMenu getEditMenu() {
        JMenu menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);
        menu.getAccessibleContext().setAccessibleDescription("Open edit menu");

        JMenuItem settings = new JMenuItem("Settings...", KeyEvent.VK_S);
        settings.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
        settings.getAccessibleContext().setAccessibleDescription("Edit Sharer settings");
        settings.setToolTipText("Edit Sharer settings");
        settings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog settingsDialog = new JDialog(Overview.this, "Settings", true /*modal*/);
                settingsDialog.setMinimumSize(new Dimension(640, 480));
                settingsDialog.setLocationRelativeTo(null);
                settingsDialog.getContentPane().add(new Settings(settingsDialog));
                settingsDialog.pack();
                settingsDialog.setVisible(true);
            }
        });
        menu.add(settings);

        return menu;
    }

    private JMenu getFileMenu() {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menu.getAccessibleContext().setAccessibleDescription("Open file menu");

        JMenuItem open = new JMenuItem("Open...", KeyEvent.VK_O);
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
        open.getAccessibleContext().setAccessibleDescription("Open shared file settings");
        open.setToolTipText("Open shared file settings");
        open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setFileHidingEnabled(false);
                int returnVal = fc.showOpenDialog(Overview.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    Path sharerFile = Paths.get(fc.getSelectedFile().getAbsolutePath());
                    SHARER_CONTROLLER.openSharerFile(sharerFile);
                }
            }
        });
        menu.add(open);

        JMenuItem loadSharerObjects = new JMenuItem("Load Sharer files", KeyEvent.VK_L);
        loadSharerObjects.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
        loadSharerObjects.getAccessibleContext().setAccessibleDescription("Load shared file settings");
        loadSharerObjects.setToolTipText("Load shared file settings");
        loadSharerObjects.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SHARER_CONTROLLER.loadSharerFiles();
            }
        });
        menu.add(loadSharerObjects);

        JMenuItem saveSharerObjects = new JMenuItem("Save shared files", KeyEvent.VK_S);
        saveSharerObjects.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
        saveSharerObjects.getAccessibleContext().setAccessibleDescription("Save shared file settings");
        saveSharerObjects.setToolTipText("Save shared file settings");
        saveSharerObjects.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SHARER_CONTROLLER.saveSharerFiles();
            }
        });
        menu.add(saveSharerObjects);

        JMenuItem close = new JMenuItem("Exit", KeyEvent.VK_X);
        close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
        close.getAccessibleContext().setAccessibleDescription("Close Sharer");
        close.setToolTipText("Close Sharer application");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Overview.this.dispose();
                System.exit(0);
            }
        });
        menu.add(close);

        return menu;
    }
}