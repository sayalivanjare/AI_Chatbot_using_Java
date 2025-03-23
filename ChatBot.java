import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import org.json.*;

public class ChatBot {
    private static JTextPane chatArea;
    private static JTextField userInput;
    private static JLabel typingIndicator;
    private static JScrollPane scrollPane;
    private static boolean darkMode = true;
    private static final String API_KEY = "USE_YOUR_KEY";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatBot::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("✨ AI ChatBot ✨");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 600);
        frame.setLayout(new BorderLayout());

        // Chat Display Area with Black/White Background
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setContentType("text/html");
        chatArea.setEditorKit(new HTMLEditorKit());
        chatArea.setBackground(darkMode ? Color.BLACK : Color.WHITE);
        chatArea.setForeground(darkMode ? Color.WHITE : Color.BLACK);

        // Scroll Pane
        scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));

        // Typing Indicator
        typingIndicator = new JLabel(" ", SwingConstants.LEFT);
        typingIndicator.setForeground(Color.ORANGE);
        typingIndicator.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        // User Input Field
        userInput = new JTextField();
        userInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userInput.setBorder(new LineBorder(Color.GRAY, 1, true));
        userInput.setPreferredSize(new Dimension(350, 40));

        // Input Panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        inputPanel.setBackground(darkMode ? Color.BLACK : Color.WHITE);
        
        // Load and scale the send button icon
        JButton sendButton = new JButton("➤");
        sendButton.setOpaque(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setBorderPainted(false);

        // Dark Mode Toggle Button
        JButton toggleTheme = new JButton("🌙");
        toggleTheme.setBorderPainted(false);
        toggleTheme.setFocusPainted(false);
        toggleTheme.addActionListener(e -> toggleDarkMode());
        
        inputPanel.add(userInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(toggleTheme, BorderLayout.WEST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(typingIndicator, BorderLayout.NORTH);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        sendButton.addActionListener(e -> sendMessage());
        userInput.addActionListener(e -> sendMessage());

        applyTheme();
        appendToChat("🤖 Bot", "Hello! Ask me anything.", "#FFA500");
    }

    private static void toggleDarkMode() {
        darkMode = !darkMode;
        applyTheme();
    }

    private static void applyTheme() {
        Color bgColor = darkMode ? Color.BLACK : Color.WHITE;
        Color textColor = darkMode ? Color.WHITE : Color.BLACK;
        chatArea.setBackground(bgColor);
        chatArea.setForeground(textColor);
        userInput.setBackground(bgColor);
        userInput.setForeground(textColor);
        userInput.setCaretColor(textColor);
    }

    private static void appendToChat(String sender, String message, String color) {
        SwingUtilities.invokeLater(() -> {
            try {
                HTMLDocument doc = (HTMLDocument) chatArea.getDocument();
                HTMLEditorKit kit = (HTMLEditorKit) chatArea.getEditorKit();
                String formattedMessage = "<div style='border-radius:10px; padding:10px; margin:5px; background:" +
                        (sender.equals("🤖 Bot") ? "#333333" : "#006666") + "; color:white;'><b>" + sender + ":</b> " + message + "</div>";
                kit.insertHTML(doc, doc.getLength(), formattedMessage, 0, 0, null);
                chatArea.setCaretPosition(doc.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void sendMessage() {
        String userMessage = userInput.getText().trim();
        if (userMessage.isEmpty()) return;
        appendToChat("👤  You", userMessage, "#00FFFF");
        userInput.setText("");
        typingIndicator.setText("🤖 Bot is typing...");

        new Thread(() -> {
            String botResponse = getGeminiResponse(userMessage);
            typingIndicator.setText("");
            appendToChat("🤖 Bot", botResponse, "#FFA500");
        }).start();
    }

    private static String getGeminiResponse(String message) {
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=" + API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String requestBody = "{ \"contents\": [ { \"role\": \"user\", \"parts\": [ { \"text\": \"" + message + "\" } ] } ] }";

            OutputStream os = conn.getOutputStream();
            os.write(requestBody.getBytes());
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return "API Error: " + responseCode;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseStr = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                responseStr.append(responseLine);
            }
            br.close();

            // Debug: Print raw API response
            System.out.println("Raw API Response: " + responseStr.toString());

            JSONObject jsonResponse = new JSONObject(responseStr.toString());
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                return parts.getJSONObject(0).getString("text");
            } else {
                return "No response from AI.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching response.";
        }
    }

}
