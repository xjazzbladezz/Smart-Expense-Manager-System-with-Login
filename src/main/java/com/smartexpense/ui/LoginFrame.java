package com.smartexpense.ui;

import com.smartexpense.db.UserDAO;
import com.smartexpense.model.User;
import com.smartexpense.service.AppContext;
import com.smartexpense.ui.components.FancyButton;
import com.smartexpense.ui.components.Toast;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

/** Split-screen sign-in / sign-up window with an animated aurora brand panel. */
public class LoginFrame extends JFrame {

    private final JTextField fullName = new JTextField();
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JLabel title = new JLabel("Welcome back");
    private final JLabel subtitle = new JLabel("Sign in to continue to your dashboard");
    private final JLabel error = new JLabel(" ");
    private final JLabel nameLabel = label("Full name");
    private final FancyButton submit = new FancyButton("Sign in", FancyButton.Style.PRIMARY);
    private final JLabel switchLink = new JLabel();
    private boolean register;

    public LoginFrame() {
        super("SmartSpend — Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);
        setLayout(new java.awt.GridLayout(1, 2));
        add(new BrandPanel());
        add(form());
        setSize(1020, 640);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        applyMode();
    }

    private static JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.MUTED);
        return l;
    }

    private JPanel form() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG);
        p.setBorder(BorderFactory.createEmptyBorder(30, 64, 30, 64));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;

        title.setFont(Theme.font(Font.BOLD, 30));
        title.setForeground(Theme.TEXT);
        subtitle.setFont(Theme.font(Font.PLAIN, 14));
        subtitle.setForeground(Theme.MUTED);
        gc.insets = new Insets(0, 0, 4, 0);
        p.add(title, gc);
        gc.gridy++;
        gc.insets = new Insets(0, 0, 28, 0);
        p.add(subtitle, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 0, 0, 0);
        p.add(nameLabel, gc);
        gc.gridy++;
        gc.insets = new Insets(5, 0, 14, 0);
        fullName.putClientProperty("JTextField.placeholderText", "e.g. Surya Chandra");
        p.add(fullName, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 0, 0, 0);
        p.add(label("Username"), gc);
        gc.gridy++;
        gc.insets = new Insets(5, 0, 14, 0);
        username.putClientProperty("JTextField.placeholderText", "your username");
        p.add(username, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 0, 0, 0);
        p.add(label("Password"), gc);
        gc.gridy++;
        gc.insets = new Insets(5, 0, 8, 0);
        password.putClientProperty("JTextField.placeholderText", "••••••••");
        password.putClientProperty("JPasswordField.showRevealButton", true);
        p.add(password, gc);

        for (JTextField f : new JTextField[]{fullName, username, password}) {
            f.setPreferredSize(new Dimension(300, 44));
            f.addActionListener(e -> submit());
        }

        gc.gridy++;
        gc.insets = new Insets(0, 0, 10, 0);
        error.setForeground(Theme.RED);
        error.setFont(Theme.font(Font.PLAIN, 12));
        p.add(error, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 0, 18, 0);
        submit.setPreferredSize(new Dimension(300, 46));
        submit.addActionListener(e -> submit());
        p.add(submit, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 0, 0, 0);
        switchLink.setFont(Theme.font(Font.PLAIN, 13));
        switchLink.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        switchLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                register = !register;
                error.setText(" ");
                applyMode();
            }
        });
        p.add(switchLink, gc);

        gc.gridy++;
        gc.insets = new Insets(26, 0, 0, 0);
        JLabel hint = new JLabel("Demo login:  admin  /  admin123");
        hint.setFont(Theme.font(Font.PLAIN, 12));
        hint.setForeground(Theme.MUTED);
        p.add(hint, gc);
        return p;
    }

    private void applyMode() {
        title.setText(register ? "Create account" : "Welcome back");
        subtitle.setText(register ? "Start tracking your money in under a minute" : "Sign in to continue to your dashboard");
        submit.setText(register ? "Create account" : "Sign in");
        nameLabel.setVisible(register);
        fullName.setVisible(register);
        switchLink.setText("<html><span style='color:#8B93AB'>" + (register ? "Already have an account? " : "New here? ")
                + "</span><span style='color:#9C84FF'><b>" + (register ? "Sign in" : "Create an account") + "</b></span></html>");
        setTitle(register ? "SmartSpend — Create account" : "SmartSpend — Sign in");
        revalidate();
        repaint();
    }

    private void submit() {
        String u = username.getText().trim();
        String pw = new String(password.getPassword());
        String name = fullName.getText().trim();
        if (u.isEmpty() || pw.isEmpty()) {
            error.setText("Enter your username and password");
            return;
        }
        if (register) {
            if (name.isEmpty()) {
                error.setText("Enter your full name");
                return;
            }
            if (u.length() < 3 || !u.matches("[A-Za-z0-9_.]+")) {
                error.setText("Username: 3+ letters, digits, _ or .");
                return;
            }
            if (pw.length() < 6) {
                error.setText("Password must be at least 6 characters");
                return;
            }
        }
        submit.setEnabled(false);
        error.setText(" ");
        final boolean reg = register;
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                UserDAO dao = new UserDAO();
                return reg ? dao.register(u, pw, name) : dao.authenticate(u, pw);
            }

            @Override
            protected void done() {
                submit.setEnabled(true);
                try {
                    User user = get();
                    if (user == null) {
                        error.setText(reg ? "That username is already taken" : "Invalid username or password");
                        return;
                    }
                    open(user);
                } catch (Exception ex) {
                    error.setText("Database error: " + DataPanel.rootMessage(ex));
                }
            }
        }.execute();
    }

    private void open(User user) {
        AppContext.init(user);
        try {
            AppContext.expenses().reload();
            AppContext.budgets().load();
        } catch (Exception ex) {
            error.setText("Could not load data: " + DataPanel.rootMessage(ex));
            return;
        }
        MainFrame main = new MainFrame(user);
        main.setVisible(true);
        dispose();
        if (AppContext.expenses().isEmpty()) {
            SwingUtilities.invokeLater(() -> Toast.show(main.getRootPane(),
                    "Welcome! Open \"Excel Data\" to load demo data or import a sheet.", Toast.Kind.INFO));
        }
    }

    // ================================================================== brand panel

    private static final class BrandPanel extends JPanel {
        private final Timer timer;
        private float t;
        private final float[][] blobs = new float[5][];

        BrandPanel() {
            setOpaque(true);
            Random r = new Random(7);
            for (int i = 0; i < blobs.length; i++) {
                blobs[i] = new float[]{r.nextFloat(), r.nextFloat(), 0.5f + r.nextFloat() * 0.5f,
                        r.nextFloat() * 6.28f, 0.2f + r.nextFloat() * 0.5f};
            }
            timer = new Timer(33, e -> {
                t += 0.012f;
                repaint();
            });
        }

        @Override
        public void addNotify() {
            super.addNotify();
            timer.start();
        }

        @Override
        public void removeNotify() {
            timer.stop();
            super.removeNotify();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(0x1A1046), w, h, new Color(0x0B1B3A)));
            g.fillRect(0, 0, w, h);

            Color[] cols = {Theme.ACCENT, Theme.ACCENT_2, Theme.PINK, new Color(0x4F8CFF), Theme.GREEN};
            for (int i = 0; i < blobs.length; i++) {
                float[] b = blobs[i];
                double x = w * (0.5 + 0.42 * Math.sin(t * b[4] + b[3]) * (0.4 + b[0]));
                double y = h * (0.5 + 0.42 * Math.cos(t * b[4] * 0.8 + b[3]) * (0.4 + b[1]));
                float rad = Math.max(w, h) * 0.42f * b[2];
                g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), rad, new float[]{0f, 1f},
                        new Color[]{Theme.alpha(cols[i], 120), Theme.alpha(cols[i], 0)}));
                g.fill(new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2));
            }

            // logo
            g.setPaint(new GradientPaint(48, 48, Theme.ACCENT, 100, 100, Theme.ACCENT_2));
            g.fill(new RoundRectangle2D.Double(48, 48, 52, 52, 18, 18));
            Icons.draw(g, "wallet", 58, 58, 32, Color.WHITE);
            g.setFont(Theme.font(Font.BOLD, 22));
            g.setColor(Color.WHITE);
            g.drawString("SmartSpend", 114, 82);

            // headline
            g.setFont(Theme.font(Font.BOLD, 40));
            g.setColor(Color.WHITE);
            g.drawString("Know where every", 48, h / 2 - 20);
            g.drawString("rupee goes.", 48, h / 2 + 28);
            g.setFont(Theme.font(Font.PLAIN, 15));
            g.setColor(new Color(255, 255, 255, 170));
            g.drawString("Track expenses, set budgets and get smart insights", 48, h / 2 + 64);
            g.drawString("— powered by Swing, JDBC, MySQL and Excel.", 48, h / 2 + 86);

            // feature chips
            String[] chips = {"Live charts", "Budget alerts", "Undo / Redo", "Excel import"};
            int x = 48;
            int y = h - 90;
            g.setFont(Theme.font(Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            for (String c : chips) {
                int cw = fm.stringWidth(c) + 26;
                if (x + cw > w - 30) {
                    x = 48;
                    y += 36;
                }
                g.setColor(new Color(255, 255, 255, 28));
                g.fill(new RoundRectangle2D.Double(x, y, cw, 28, 28, 28));
                g.setColor(new Color(255, 255, 255, 60));
                g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, cw - 1, 27, 28, 28));
                g.setColor(Color.WHITE);
                g.drawString(c, x + 13, y + 19);
                x += cw + 10;
            }
            g.dispose();
        }
    }
}
