package team3.gui;

import team3.database.DBQueries;
import team3.database.SQLite;
import team3.email.EmailSender;
import team3.gui.IconButton.*;
import team3.gui.checkboxFactory.*;

import team3.gui.IconButton.*;
import team3.gui.checkboxFactory.autoUpdateNews;
import team3.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team3.gui.checkboxFactory.autoSend;
import team3.gui.checkboxFactory.news_onlyNew;
import team3.gui.checkboxFactory.todayOrNotCheckbox;
import team3.search.Search;
import team3.utils.Common;
import team3.utils.ExportToExcel;
import team3.utils.MyTimerTask;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class Gui extends JFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gui.class);
    final SQLite sqlite = new SQLite();
    final DBQueries dbQueries = new DBQueries();
    final Search search = new Search();
    private static final Font GUI_FONT = new Font("Tahoma", Font.PLAIN, 11);
    private static final String[] INTERVALS = {"1 min", "5 min", "15 min", "30 min", "45 min", "1 hour", "2 hours",
            "4 hours", "8 hours", "12 hours", "24 hours", "48 hours"};
    private static final long AUTO_START_TIMER = 60000L; // 60 секунд
    public static final ImageIcon LOGO_ICON = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/logo.png")));
    public static final ImageIcon SEND_ICON = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/send.png")));
    public static final ImageIcon WHEN_MOUSE_ON_SEND_ICON = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/send2.png")));
    public static final ImageIcon WHEN_SENT_ICON = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/send3.png"))); 
    public static final ImageIcon SEARCH_ICON = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/search.png")));
    public static final ImageIcon STOP_ICON = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/stop.png")));
    public static final ImageIcon CLEAR_ICON = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/clear.png")));
    public static final ImageIcon EXCEL_ICON = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/excel.png")));
    public static final ImageIcon CREATE_ICON = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/create.png")));
    public static final ImageIcon DELETE_ICON = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/delete.png")));
    public static final ImageIcon FONT_ICON = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/font.png")));
    public static final ImageIcon BG_ICON = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/bg.png")));
    public static int newsCount = 1;
    public static boolean isOnlyLastNews = false;
    public static boolean isInKeywords = false;
    public static String findWord = "";
    //public static String sendTo;
    public static JScrollPane scrollPane;
    public static JTable table;
    public static JTable tableForAnalysis;
    public static DefaultTableModel model;
    public static DefaultTableModel modelForAnalysis;
    public static JTextField topKeyword;
    public static JTextField sendEmailTo;
    public static JTextField addKeywordToList;
    public static JTextArea consoleTextArea;
    public static JComboBox<String> keywords;
    public static JComboBox<String> newsInterval;
    public static JLabel labelSign;
    public static JLabel labelSum;
    //public static JLabel labelInfo;
    public static JLabel lblLogSourceSqlite;
    public static JButton searchBtnTop;
    public static JButton searchBtnBottom;
    public static JButton stopBtnTop;
    public static JButton stopBtnBottom;
    public static JButton sendEmailBtn;
    public static JButton smiBtn;
    public static JButton logBtn;
    public static JButton exclBtn;
    public static Checkbox todayOrNotCbx;
    public static Checkbox autoUpdateNewsTop;
    public static Checkbox autoUpdateNewsBottom;
    public static Checkbox autoSendMessage;
    public static Checkbox onlyNewNews;
    public static JProgressBar progressBar;
    public static Timer timer;
    public static TimerTask timerTask;
    public static final AtomicBoolean WAS_CLICK_IN_TABLE_FOR_ANALYSIS = new AtomicBoolean(false);
    public static final AtomicBoolean GUI_IN_TRAY = new AtomicBoolean(false);

    public Gui() {
        setResizable(false);
        getContentPane().setBackground(new Color(42, 42, 42));
        setTitle("Avandy News");
        setIconImage(LOGO_ICON.getImage());
        setFont(GUI_FONT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(340, 100, 1195, 634);
        getContentPane().setLayout(null);

        //Action Listener for EXIT_ON_CLOSE
        addWindowListener(new WindowAdapter() {
            // закрытие окна
            @Override
            public void windowClosing(WindowEvent e) {
                Search.isSearchFinished.set(true);
                SQLite.isConnectionToSQLite = false;
                Common.saveState();
                LOGGER.info("Application closed");
                if (SQLite.isConnectionToSQLite) sqlite.closeSQLiteConnection();
            }

            // сворачивание в трей
            @Override
            public void windowIconified(WindowEvent pEvent) {
                GUI_IN_TRAY.set(true);
                setVisible(false);
                if (autoUpdateNewsBottom.getState()) consoleTextArea.setText("");
            }

            // разворачивание из трея
            public void windowDeiconified(WindowEvent pEvent) {
                GUI_IN_TRAY.set(false);
            }
        });

        // Сворачивание приложения в трей
        try {
            BufferedImage Icon = ImageIO.read(Objects.requireNonNull(Gui.class.getResourceAsStream("/icons/logo.png")));
            final TrayIcon trayIcon = new TrayIcon(Icon, "Avandy News");
            SystemTray systemTray = SystemTray.getSystemTray();
            systemTray.add(trayIcon);

            final PopupMenu trayMenu = new PopupMenu();
            MenuItem itemShow = new MenuItem("Show");
            itemShow.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
            });
            trayMenu.add(itemShow);

            MenuItem itemClose = new MenuItem("Close");
            itemClose.addActionListener(e -> System.exit(0));
            trayMenu.add(itemClose);

            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        setVisible(true);
                        setExtendedState(JFrame.NORMAL);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        trayIcon.setPopupMenu(trayMenu);
                    }
                }
            });
        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }

        //Input keyword
        JLabel lblNewLabel = new JLabel("Keyword:");
        lblNewLabel.setForeground(new Color(255, 179, 131));
        lblNewLabel.setBounds(10, 9, 71, 19);
        lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
        lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
        getContentPane().add(lblNewLabel);

        //Table
        scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 40, 860, 500);
        getContentPane().add(scrollPane);
        Object[] columns = {"Num", "Source", "Title (double click to open the link)", "Date", "Link"};
        model = new DefaultTableModel(new Object[][]{
        }, columns) {
            final boolean[] columnEditable = new boolean[]{
                    false, false, false, false, false
            };

            public boolean isCellEditable(int row, int column) {
                return columnEditable[column];
            }

            // Сортировка
            final Class[] types_unique = {Integer.class, String.class, String.class, /*Date.class*/ String.class, String.class};

            @Override
            public Class getColumnClass(int columnIndex) {
                return this.types_unique[columnIndex];
            }
        };
        table = new JTable(model) {
            // tooltips
            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = 2;
                try {
                    tip = (String) getValueAt(rowIndex, colIndex);
                } catch (RuntimeException ignored) {
                }
                assert tip != null;
                if (tip.length() > 80) {
                    return tip;
                } else return null;
            }
        };
        //headers
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Tahoma", Font.BOLD, 13));
        //Cell alignment
        DefaultTableCellRenderer Renderer = new DefaultTableCellRenderer();
        Renderer.setHorizontalAlignment(JLabel.CENTER);

        table.getColumnModel().getColumn(0).setCellRenderer(Renderer);
        table.setRowHeight(28);
        table.setColumnSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setFont(new Font("Tahoma", Font.PLAIN, 14));
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setMaxWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(490);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setMaxWidth(100);
        table.removeColumn(table.getColumnModel().getColumn(4)); // Скрыть 4 колонку со ссылкой на новость
        scrollPane.setViewportView(table);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.convertRowIndexToModel(table.rowAtPoint(new Point(e.getX(), e.getY()))); // при сортировке строк оставляет верные данные
                    int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
                    if (col == 2 || col == 4) {
                        String url = (String) table.getModel().getValueAt(row, 4);
                        URI uri = null;
                        try {
                            uri = new URI(url);
                        } catch (URISyntaxException ex) {
                            ex.printStackTrace();
                            LOGGER.warn(ex.getMessage());
                        }
                        Desktop desktop = Desktop.getDesktop();
                        assert uri != null;
                        try {
                            desktop.browse(uri);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            LOGGER.warn(ex.getMessage());
                        }
                    }
                }
            }
        });

        //Table for analysis
        JScrollPane scrollForAnalysis = new JScrollPane();
        scrollForAnalysis.setBounds(880, 40, 290, 236);
        getContentPane().add(scrollForAnalysis);

        String[] columnsForAnalysis = {"top 10", "freq.", " "};
        modelForAnalysis = new DefaultTableModel(new Object[][]{}, columnsForAnalysis) {
            final boolean[] column_for_analysis = new boolean[]{false, false, true};

            public boolean isCellEditable(int row, int column) {
                return column_for_analysis[column];
            }

            // Сортировка
            final Class[] types_unique = {String.class, Integer.class, Button.class};

            @Override
            public Class getColumnClass(int columnIndex) {
                return this.types_unique[columnIndex];
            }
        };
        tableForAnalysis = new JTable(modelForAnalysis);
        JTableHeader header_for_analysis = tableForAnalysis.getTableHeader();
        header_for_analysis.setFont(new Font("Tahoma", Font.BOLD, 13));
        //Cell alignment
        DefaultTableCellRenderer rendererForAnalysis = new DefaultTableCellRenderer();
        rendererForAnalysis.setHorizontalAlignment(JLabel.CENTER);
        tableForAnalysis.getColumnModel().getColumn(1).setCellRenderer(rendererForAnalysis);
        //tableForAnalysis.getColumnModel().getColumn(1).setCellRenderer(rendererForAnalysis);
        tableForAnalysis.getColumn(" ").setCellRenderer(new ButtonColumn(tableForAnalysis, 2));
        tableForAnalysis.setRowHeight(21);
        tableForAnalysis.setColumnSelectionAllowed(true);
        tableForAnalysis.setCellSelectionEnabled(true);
        tableForAnalysis.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tableForAnalysis.setFont(new Font("Tahoma", Font.PLAIN, 14));
        tableForAnalysis.getColumnModel().getColumn(0).setPreferredWidth(140);
        tableForAnalysis.getColumnModel().getColumn(1).setPreferredWidth(40);
        tableForAnalysis.getColumnModel().getColumn(1).setMaxWidth(40);
        tableForAnalysis.getColumnModel().getColumn(2).setMaxWidth(30);
        scrollForAnalysis.setViewportView(tableForAnalysis);

        // запуск поиска по слову из таблицы анализа
        tableForAnalysis.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tableForAnalysis.convertRowIndexToModel(tableForAnalysis.rowAtPoint(new Point(e.getX(), e.getY())));
                    int col = tableForAnalysis.columnAtPoint(new Point(e.getX(), e.getY()));
                    if (col == 0) {
                        Gui.topKeyword.setText((String) tableForAnalysis.getModel().getValueAt(row, 0));
                        searchBtnTop.doClick();
                        WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(true);
                    }
                }
            }
        });

        //Keyword field
        topKeyword = new JTextField(findWord);
        topKeyword.setBounds(87, 9, 99, 21);
        topKeyword.setFont(new Font("Tahoma", Font.BOLD, 13));
        getContentPane().add(topKeyword);

        //Search addNewSource
        searchButtonTop _searchButtonTop = new searchButtonTop(SEARCH_ICON, new Color(154, 237, 196), new Font("Tahoma", Font.BOLD, 10), 192, 9);
        searchBtnTop = new JButton("");//_searchButtonTop.makeButton();
        _searchButtonTop.buttonSetting(searchBtnTop);
        getContentPane().add(searchBtnTop);
        // Search by Enter
        getRootPane().setDefaultButton(searchBtnTop);
        searchBtnTop.requestFocus();
        searchBtnTop.doClick();
        searchBtnTop.addActionListener(e -> new Thread(() -> search.mainSearch("word")).start());

        //Stop addNewSource
        stopButtonTop _stopButtonTop = new stopButtonTop(STOP_ICON, new Color(255, 208, 202), 192, 9);
        stopBtnTop = new JButton("");
        _stopButtonTop.buttonSetting(stopBtnTop);
        stopBtnTop.addActionListener(e -> {
            try {
                Search.isSearchFinished.set(true);
                Search.isStop.set(true);
                Common.console("status: search stopped");
                searchBtnTop.setVisible(true);
                stopBtnTop.setVisible(false);
                Search.isSearchNow.set(false);
                try {
                    String q_begin = "ROLLBACK";
                    Statement st_begin = SQLite.connection.createStatement();
                    st_begin.executeUpdate(q_begin);
                } catch (SQLException ignored) {
                }
            } catch (Exception t) {
                Common.console("status: there is no threads to stop");
            }
        });
        getContentPane().add(stopBtnTop);

        //Amount of news
        labelSum = new JLabel();
        labelSum.setBounds(880, 278, 120, 13);
        labelSum.setFont(GUI_FONT);
        labelSum.setForeground(new Color(255, 255, 153));
        labelSum.setBackground(new Color(240, 255, 240));
        getContentPane().add(labelSum);

        /* Top-Right buttons */
        // Выбор цвета фона
        backGroundColorButton _backGroundColorButton = new backGroundColorButton(BG_ICON, new Color(189, 189, 189), 1035, 9);
        JButton backgroundColorBtn = new JButton();
        _backGroundColorButton.buttonSetting(backgroundColorBtn);
        backgroundColorBtn.addActionListener(e -> {
            Color color = JColorChooser.showDialog(null, "Color", Color.black);
            if (color != null) {
                try {
                    table.setBackground(color);
                    Common.delSettings("backgroundColorRed");
                    Common.delSettings("backgroundColorGreen");
                    Common.delSettings("backgroundColorBlue");
                    Common.writeToConfig(String.valueOf(color.getRed()), "backgroundColorRed");
                    Common.writeToConfig(String.valueOf(color.getGreen()), "backgroundColorGreen");
                    Common.writeToConfig(String.valueOf(color.getBlue()), "backgroundColorBlue");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        getContentPane().add(backgroundColorBtn);

        // Выбор цвета шрифта в таблице
        fontColorButton _fontColorButton = new fontColorButton(FONT_ICON, new Color(190, 225, 255), 1070, 9);
        JButton fontColorBtn = new JButton();
        _fontColorButton.buttonSetting(fontColorBtn);
        fontColorBtn.addActionListener(e -> {
            Color color = JColorChooser.showDialog(null, "Color", Color.black);
            if (color != null) {
                try {
                    table.setForeground(color);
                    tableForAnalysis.setForeground(color);
                    Common.delSettings("fontColorRed");
                    Common.delSettings("fontColorGreen");
                    Common.delSettings("fontColorBlue");
                    Common.writeToConfig(String.valueOf(color.getRed()), "fontColorRed");
                    Common.writeToConfig(String.valueOf(color.getGreen()), "fontColorGreen");
                    Common.writeToConfig(String.valueOf(color.getBlue()), "fontColorBlue");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        getContentPane().add(fontColorBtn);

        //Export to excel
        exportButton _exportButton = new exportButton(EXCEL_ICON, new Color(255, 251, 183), 1105, 9);
        JButton exportBtn = new JButton();
        _exportButton.buttonSetting(exportBtn);
        exportBtn.addActionListener(e -> {
            if (model.getRowCount() != 0) {
                new Thread(new ExportToExcel()::exportResultsToExcel).start();
                Common.console("status: export");
            } else {
                Common.console("status: there is no data to export");
            }
        });
        getContentPane().add(exportBtn);

        //Clear addNewSource
        clearButtonTop _clearButtonTop = new clearButtonTop(CLEAR_ICON, new Color(250, 128, 114), 1140, 9);
        JButton clearBtnTop = new JButton();
        _clearButtonTop.buttonSetting(clearBtnTop);
        clearBtnTop.addActionListener(e -> {
            try {
                if (model.getRowCount() == 0) {
                    Common.console("status: no data to clear");
                    return;
                }
                //labelInfo.setText("");
                Search.j = 1;
                model.setRowCount(0);
                modelForAnalysis.setRowCount(0);
                newsCount = 0;
                labelSum.setText("" + newsCount);
                Common.console("status: clear");
            } catch (Exception t) {
                Common.console(t.getMessage());
                t.printStackTrace();
                LOGGER.warn(t.getMessage());
            }
        });
        getContentPane().add(clearBtnTop);

        /* KEYWORDS SEARCH */
        // label
        JLabel lblKeywordsSearch = new JLabel();
        lblKeywordsSearch.setText("search by keywords");
        lblKeywordsSearch.setForeground(new Color(255, 255, 153));
        lblKeywordsSearch.setFont(GUI_FONT);
        lblKeywordsSearch.setBounds(10, 545, 160, 14);
        getContentPane().add(lblKeywordsSearch);

        //Add to combo box
        addKeywordToList = new JTextField();
        addKeywordToList.setFont(GUI_FONT);
        addKeywordToList.setBounds(9, 561, 80, 22);
        getContentPane().add(addKeywordToList);

        //Add to keywords combo box
        buttonAddKeywordToList _buttonAddKeywordToList = new buttonAddKeywordToList(CREATE_ICON, 95, 561);
        JButton btnAddKeywordToList = new JButton("");
        _buttonAddKeywordToList.buttonSetting(btnAddKeywordToList);
        btnAddKeywordToList.addActionListener(e -> {
            if (addKeywordToList.getText().length() > 0) {
                String word = addKeywordToList.getText();
                for (int i = 0; i < keywords.getItemCount(); i++) {
                    if (word.equals(keywords.getItemAt(i))) {
                        Common.console("info: список ключевых слов уже содержит: " + word);
                        isInKeywords = true;
                    } else {
                        isInKeywords = false;
                    }
                }
                if (!isInKeywords) {
                    Common.writeToConfig(word, "keyword");
                    keywords.addItem(word);
                    isInKeywords = false;
                }
                addKeywordToList.setText("");
            }
        });
        getContentPane().add(btnAddKeywordToList);

        //Delete from combo box
        buttonDeleteFromList _buttonDeleteFromList = new buttonDeleteFromList(DELETE_ICON, 130, 561);
        JButton btnDelFromList = new JButton("");
        _buttonDeleteFromList.buttonSetting(btnDelFromList);
        btnDelFromList.addActionListener(e -> {
            if (keywords.getItemCount() > 0) {
                try {
                    String item = (String) keywords.getSelectedItem();
                    keywords.removeItem(item);
                    Common.delSettings("keyword=" + Objects.requireNonNull(item));
                } catch (IOException io) {
                    io.printStackTrace();
                    LOGGER.warn(io.getMessage());
                }
            }

        });
        getContentPane().add(btnDelFromList);

        //Keywords combo box
        keywords = new JComboBox<>();
        keywords.setFont(GUI_FONT);
        keywords.setModel(new DefaultComboBoxModel<>());
        keywords.setEditable(false);
        keywords.setBounds(165, 561, 90, 22);
        getContentPane().add(keywords);

        //Bottom search by keywords
        searchButtonBottom _searchButtonBottom = new searchButtonBottom(SEARCH_ICON, new Font("Tahoma", Font.BOLD, 10), new Color(154, 237, 196), 261, 561);
        searchBtnBottom = new JButton("");
        _searchButtonBottom.buttonSetting(searchBtnBottom);
        //searchBtnBottom.addActionListener(e -> new Thread(Search::keywordsSearch).start());
        searchBtnBottom.addActionListener(e -> new Thread(() -> search.mainSearch("words")).start());
        getContentPane().add(searchBtnBottom);

        //Stop addNewSource (bottom)
        stopButtonBottom _stopButtonBottom = new stopButtonBottom(STOP_ICON, new Color(255, 208, 202), 261, 561);
        stopBtnBottom = new JButton("");
        _stopButtonBottom.buttonSetting(stopBtnBottom);
        stopBtnBottom.addActionListener(e -> {
            try {
                Search.isSearchFinished.set(true);
                Search.isStop.set(true);
                Common.console("status: search stopped");
                searchBtnBottom.setVisible(true);
                stopBtnBottom.setVisible(false);
                Search.isSearchNow.set(false);
                try {
                    String q_begin = "ROLLBACK";
                    Statement st_begin = SQLite.connection.createStatement();
                    st_begin.executeUpdate(q_begin);
                } catch (SQLException ignored) {
                }
            } catch (Exception t) {
                Common.console("status: there is no threads to stop");
            }
        });
        getContentPane().add(stopBtnBottom);

        // Автозапуск поиска по ключевым словам каждые 30 секунд
        autoUpdateNews _autoUpdateNewsBottom = new autoUpdateNews(297, 561, 75);
        autoUpdateNewsBottom = new Checkbox("auto update");
        _autoUpdateNewsBottom.checkBoxSetting(autoUpdateNewsBottom);
        autoUpdateNewsBottom.setFont(GUI_FONT);
        getContentPane().add(autoUpdateNewsBottom);
        autoUpdateNewsBottom.addItemListener(e -> {
            if (autoUpdateNewsBottom.getState()) {
                timer = new Timer(true);
                timerTask = new MyTimerTask();
                timer.scheduleAtFixedRate(timerTask, 0, AUTO_START_TIMER);
                searchBtnBottom.setVisible(false);
                stopBtnBottom.setVisible(true);
                autoUpdateNewsTop.setVisible(false);
            } else {
                timer.cancel();
                searchBtnBottom.setVisible(true);
                stopBtnBottom.setVisible(false);
                autoUpdateNewsTop.setVisible(true);
                try {
                    stopBtnTop.doClick();
                } catch (Exception ignored) {

                }
            }
        });

        /* CONSOLE */
        //Console - textarea
        consoleTextArea = new JTextArea();
        // авто скроллинг
        DefaultCaret caret = (DefaultCaret) consoleTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
        consoleTextArea.setAutoscrolls(true);
        consoleTextArea.setLineWrap(true);
        consoleTextArea.setEditable(false);
        consoleTextArea.setBounds(20, 11, 145, 51);
        consoleTextArea.setFont(GUI_FONT);
        consoleTextArea.setForeground(SystemColor.white);
        consoleTextArea.setBackground(new Color(83, 82, 82)); // 83, 82, 82
        getContentPane().add(consoleTextArea);

        //Console - scroll
        JScrollPane consoleScroll = new JScrollPane(consoleTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        consoleScroll.setBounds(879, 303, 290, 142);
        consoleScroll.setBorder(null);
        getContentPane().add(consoleScroll);
        //Console - label
        JLabel lblConsole = new JLabel();
        lblConsole.setText("clear console");
        lblConsole.setForeground(new Color(255, 255, 153));
        lblConsole.setFont(GUI_FONT);
        lblConsole.setBounds(1089, 448, 64, 14);
        getContentPane().add(lblConsole);

        // Clear console
        JButton clearConsoleBtn = new JButton();
        //clearConsoleBtn.setIcon(clearIco);
        clearConsoleBtn.setToolTipText("Clear the console");
        clearConsoleBtn.setBackground(new Color(0, 52, 96));
        clearConsoleBtn.setBounds(1155, 448, 14, 14);
        clearConsoleBtn.addActionListener(e -> consoleTextArea.setText(""));
        getContentPane().add(clearConsoleBtn);

        // Шкала прогресса
        progressBar = new JProgressBar();
        progressBar.setFocusable(false);
        progressBar.setMaximum(100);
        progressBar.setBorderPainted(false);
        progressBar.setForeground(new Color(10, 255, 41));
        progressBar.setBackground(new Color(1, 1, 1));
        progressBar.setBounds(10, 37, 860, 1);
        getContentPane().add(progressBar);

        // Интервалы для поиска новостей
        newsInterval = new JComboBox<>(INTERVALS);
        newsInterval.setFont(GUI_FONT);
        newsInterval.setBounds(516, 10, 75, 20);
        getContentPane().add(newsInterval);

        // Today or not
        todayOrNotCheckbox _todayOrNotCbx = new todayOrNotCheckbox(449, 10, 64);
        todayOrNotCbx = new Checkbox("in the last");
        todayOrNotCbx.setFont(GUI_FONT);
        _todayOrNotCbx.checkBoxSetting(todayOrNotCbx);
        todayOrNotCbx.addItemListener(e -> newsInterval.setVisible(todayOrNotCbx.getState()));
        getContentPane().add(todayOrNotCbx);

        // Автозапуск поиска по слову каждые 60 секунд
        autoUpdateNews _autoUpdateNewsTop = new autoUpdateNews(297, 10, 75);
        autoUpdateNewsTop = new Checkbox("auto update");
        _autoUpdateNewsTop.checkBoxSetting(autoUpdateNewsTop);
        autoUpdateNewsTop.setFont(GUI_FONT);
        getContentPane().add(autoUpdateNewsTop);
        autoUpdateNewsTop.addItemListener(e -> {
            if (autoUpdateNewsTop.getState()) {
                timer = new Timer(true);
                timerTask = new MyTimerTask();
                timer.scheduleAtFixedRate(timerTask, 0, AUTO_START_TIMER);
                searchBtnTop.setVisible(false);
                stopBtnTop.setVisible(true);
                autoUpdateNewsBottom.setVisible(false);
            } else {
                timer.cancel();
                searchBtnTop.setVisible(true);
                stopBtnTop.setVisible(false);
                autoUpdateNewsBottom.setVisible(true);
                stopBtnTop.doClick();
            }
        });

        // Диалоговое окно со списком исключенных слов из анализа
        exclBtn = new JButton();
        exclBtn.setFocusable(false);
        exclBtn.setContentAreaFilled(true);
        //exclBtn.setBorderPainted(false);
        exclBtn.setBackground(new Color(0, 52, 96));
        exclBtn.setBounds(1157, 278, 14, 14);
        getContentPane().add(exclBtn);
        exclBtn.addActionListener((e) -> new Dialogs("exclDlg"));
        exclBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на кнопку
            @Override
            public void mouseEntered(MouseEvent e) {
                exclBtn.setBackground(new Color(128, 128, 128));
            }

            @Override
            // убрали мышку с кнопки
            public void mouseExited(MouseEvent e) {
                exclBtn.setBackground(new Color(0, 52, 96));
            }
        });
        //label
        JLabel excludedLabel = new JLabel("excluded list");
        excludedLabel.setHorizontalAlignment(SwingConstants.LEFT);
        excludedLabel.setForeground(new Color(255, 255, 153));
        excludedLabel.setFont(GUI_FONT);
        excludedLabel.setBackground(new Color(240, 255, 240));
        excludedLabel.setBounds(1092, 278, 64, 14);
        getContentPane().add(excludedLabel);

        /* BOTTOM RIGHT AREA */
        //send e-mail to - label
        JLabel lblSendToEmail = new JLabel();
        lblSendToEmail.setText("send to");
        lblSendToEmail.setForeground(new Color(255, 255, 153));
        lblSendToEmail.setFont(GUI_FONT);
        lblSendToEmail.setBounds(880, 504, 36, 14);
        getContentPane().add(lblSendToEmail);

        //send e-mail to
        sendEmailTo = new JTextField("enter your email");
        sendEmailTo.setBounds(879, 519, 142, 21);
        sendEmailTo.setFont(GUI_FONT);
        getContentPane().add(sendEmailTo);

        //Send current results e-mail
        sendEmailBtn = new JButton();
        sendEmailBtn.setIcon(SEND_ICON);
        sendEmailBtn.setToolTipText("send the current result");
        sendEmailBtn.setFocusable(false);
        sendEmailBtn.setContentAreaFilled(false);
        sendEmailBtn.setBorderPainted(false);
        sendEmailBtn.setBackground(new Color(255, 255, 153));
        sendEmailBtn.setBounds(1020, 518, 32, 23);
        sendEmailBtn.addActionListener(e -> {
            if (model.getRowCount() > 0 && sendEmailTo.getText().contains("@")) {
                Common.console("status: sending e-mail");
                //sendTo = sendEmailTo.getText();
                Common.IS_SENDING.set(false);
                new Thread(Common::fill).start();
                EmailSender email = new EmailSender();
                new Thread(email::sendMessage).start();
            }
        });
        sendEmailBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на письмо
            @Override
            public void mouseEntered(MouseEvent e) {
                if (sendEmailBtn.getIcon() == SEND_ICON) {
                    sendEmailBtn.setIcon(WHEN_MOUSE_ON_SEND_ICON);
                }
            }

            @Override
            // убрали мышку с письма
            public void mouseExited(MouseEvent e) {
                if (sendEmailBtn.getIcon() == WHEN_MOUSE_ON_SEND_ICON) {
                    sendEmailBtn.setIcon(SEND_ICON);
                }
            }

        });
        getContentPane().add(sendEmailBtn);

        // Автоматическая отправка письма с результатами
        autoSend _autoSendMessage = new autoSend(378, 10, 66);
        autoSendMessage = new Checkbox("auto send");
        _autoSendMessage.checkBoxSetting(autoSendMessage);
        autoSendMessage.setFont(GUI_FONT);
        getContentPane().add(autoSendMessage);

        // Диалоговое окно со списком источников "sources"
        smiBtn = new JButton();
        smiBtn.setFocusable(false);
        smiBtn.setContentAreaFilled(true);
        smiBtn.setBorderPainted(false);
        smiBtn.setFocusable(false);
        smiBtn.setBounds(883, 479, 14, 14);
        smiBtn.setBackground(new Color(221, 255, 221));
        getContentPane().add(smiBtn);
        smiBtn.addActionListener((e) -> new Dialogs("smiDlg"));
        smiBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на кнопку
            @Override
            public void mouseEntered(MouseEvent e) {
                smiBtn.setBackground(new Color(25, 226, 25));
                lblLogSourceSqlite.setText("sources");
            }

            @Override
            // убрали мышку с кнопки
            public void mouseExited(MouseEvent e) {
                smiBtn.setBackground(new Color(221, 255, 221));
                lblLogSourceSqlite.setText("");
            }
        });

        // добавить новый RSS источник "add source"
        JButton addNewSource = new JButton();
        addNewSource.setFocusable(false);
        addNewSource.setContentAreaFilled(true);
        addNewSource.setBorderPainted(false);
        addNewSource.setBackground(new Color(243, 229, 255));
        addNewSource.setBounds(902, 479, 14, 14);
        getContentPane().add(addNewSource);
        addNewSource.addActionListener(e -> dbQueries.insertNewSource(SQLite.connection));
        addNewSource.addMouseListener(new MouseAdapter() {
            // наведение мышки на кнопку
            @Override
            public void mouseEntered(MouseEvent e) {
                addNewSource.setBackground(new Color(153, 84, 241));
                lblLogSourceSqlite.setText("add source");
            }

            @Override
            // убрали мышку с кнопки
            public void mouseExited(MouseEvent e) {
                addNewSource.setBackground(new Color(243, 229, 255));
                lblLogSourceSqlite.setText("");
            }
        });

        // Диалоговое окно лога "log"
        logBtn = new JButton();
        logBtn.setContentAreaFilled(true);
        logBtn.setBorderPainted(false);
        logBtn.setFocusable(false);
        logBtn.setBackground(new Color(248, 206, 165));
        logBtn.setBounds(921, 479, 14, 14);
        getContentPane().add(logBtn);
        logBtn.addActionListener(e -> new Dialogs("logDlg"));
        logBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на кнопку
            @Override
            public void mouseEntered(MouseEvent e) {
                logBtn.setBackground(new Color(222, 114, 7));
                lblLogSourceSqlite.setText("log");
            }

            @Override
            // убрали мышку с кнопки
            public void mouseExited(MouseEvent e) {
                logBtn.setBackground(new Color(248, 206, 165));
                lblLogSourceSqlite.setText("");
            }
        });

        //SQLite
        JButton sqliteBtn = new JButton();
        sqliteBtn.setToolTipText("press CTRL+V in SQLite to open the database");
        sqliteBtn.setFocusable(false);
        sqliteBtn.setContentAreaFilled(true);
        sqliteBtn.setBorderPainted(false);
        sqliteBtn.setBackground(new Color(244, 181, 181));
        sqliteBtn.setBounds(940, 479, 14, 14);
        getContentPane().add(sqliteBtn);
        sqliteBtn.addActionListener(e -> {
            // запуск SQLite
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                try {
                    Desktop.getDesktop().open(new File(Main.DIRECTORY_PATH + "sqlite3.exe"));
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }

            // копируем адрес базы в SQLite в системный буфер для быстрого доступа
            String pathToBase = (".open " + Main.DIRECTORY_PATH + "news.db").replace("\\", "/");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(pathToBase), null);
        });
        sqliteBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на кнопку
            @Override
            public void mouseEntered(MouseEvent e) {
                sqliteBtn.setBackground(new Color(255, 50, 50));
                lblLogSourceSqlite.setText("sqlite");
            }

            @Override
            // убрали мышку с кнопки
            public void mouseExited(MouseEvent e) {
                sqliteBtn.setBackground(new Color(244, 181, 181));
                lblLogSourceSqlite.setText("");
            }
        });

        //Открыть папку с настройками "files"
        JButton settingsDirectoryBtn = new JButton();
        settingsDirectoryBtn.setFocusable(false);
        settingsDirectoryBtn.setContentAreaFilled(true);
        settingsDirectoryBtn.setBorderPainted(false);
        settingsDirectoryBtn.setBackground(new Color(219, 229, 252));
        settingsDirectoryBtn.setBounds(959, 479, 14, 14);
        getContentPane().add(settingsDirectoryBtn);
        settingsDirectoryBtn.addActionListener(e -> {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                try {
                    Desktop.getDesktop().open(new File(Main.DIRECTORY_PATH));
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }

        });
        settingsDirectoryBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на кнопку
            @Override
            public void mouseEntered(MouseEvent e) {
                settingsDirectoryBtn.setBackground(new Color(80, 124, 255));
                lblLogSourceSqlite.setText("files");
            }

            @Override
            // убрали мышку с кнопки
            public void mouseExited(MouseEvent e) {
                settingsDirectoryBtn.setBackground(new Color(219, 229, 252));
                lblLogSourceSqlite.setText("");
            }
        });

        // Источники, лог, sqlite лейбл
        lblLogSourceSqlite = new JLabel();
        lblLogSourceSqlite.setForeground(Color.WHITE);
        lblLogSourceSqlite.setFont(GUI_FONT);
        lblLogSourceSqlite.setBounds(979, 479, 60, 14);
        getContentPane().add(lblLogSourceSqlite);

        // Border different bottoms
        Box queryTableBox = Box.createVerticalBox();
        queryTableBox.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
        queryTableBox.setBounds(879, 473, 290, 26);
        getContentPane().add(queryTableBox);

        // latest news
        news_onlyNew _onlyNewNews = new news_onlyNew(230, 10, 65);
        onlyNewNews = new Checkbox("only new");
        _onlyNewNews.checkBoxSetting(onlyNewNews);
        onlyNewNews.setFont(GUI_FONT);
        getContentPane().add(onlyNewNews);
        onlyNewNews.addItemListener(e -> {
            isOnlyLastNews = onlyNewNews.getState();
            if (!isOnlyLastNews) {
                dbQueries.deleteFrom256(SQLite.connection);
            }
        });

        //My sign
        labelSign = new JLabel("mrPro");
        labelSign.setForeground(new Color(255, 160, 122));
        labelSign.setEnabled(false);
        labelSign.setFont(new Font("Tahoma", Font.BOLD, 11));
        labelSign.setBounds(995, 14, 57, 14);
        getContentPane().add(labelSign);
        labelSign.addMouseListener(new MouseAdapter() {
            // наведение мышки на письмо
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!labelSign.isEnabled()) {
                    labelSign.setEnabled(true);
                }
            }

            // убрали мышку с письма
            @Override
            public void mouseExited(MouseEvent e) {
                if (labelSign.isEnabled()) {
                    labelSign.setEnabled(false);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    String url = "https://github.com/mrprogre";
                    URI uri = null;
                    try {
                        uri = new URI(url);
                    } catch (URISyntaxException ex) {
                        ex.printStackTrace();
                    }
                    Desktop desktop = Desktop.getDesktop();
                    assert uri != null;
                    try {
                        desktop.browse(uri);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        LOGGER.warn(ex.getMessage());
                    }
                }
            }
        });

        setVisible(true);
    }
}