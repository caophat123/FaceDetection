import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.awt.geom.Ellipse2D;

public class FaceDetectionClient3 {
    private static final String CASCADE_PATH = "F:\\Demojava\\.vscode\\haarcascade_frontalface_default.xml";
    private static String name;
    private static ImageIcon logoIcon;
    private static VideoCapture capture;
    private static Timer timer;

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        JFrame frame = new JFrame("Face Detection Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 800);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(new Color(220, 180, 200));

        BufferedImage logoImage = loadImage("Client2\\lib\\logoda.jpg");
        if (logoImage != null) {
            BufferedImage resizedLogo = resizeImage(logoImage, 100, 100);
            BufferedImage circularLogo = makeCircular(resizedLogo);
            logoIcon = new ImageIcon(circularLogo);
            JLabel logoLabel = new JLabel(logoIcon);
            logoPanel.add(logoLabel);
        }

        JLabel textLabel = new JLabel("Face Detection Program");
        textLabel.setFont(new Font("Arial", Font.BOLD, 20));
        textLabel.setForeground(Color.WHITE);
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoPanel.add(textLabel);

        JPanel controlsPanel = new JPanel(new BorderLayout());

        Border padding = BorderFactory.createEmptyBorder(20, 20, 20, 20);
        Border line = BorderFactory.createLineBorder(Color.RED, 3);
        Border compound = BorderFactory.createCompoundBorder(line, padding);
        mainPanel.setBorder(compound);

        JPanel buttonPanel = new JPanel();
        JButton webcamButton = new JButton("Use Webcam");
        JButton fileButton = new JButton("Choose File");
        JButton stopButton = new JButton("Stop Webcam");
        JButton exitButton = new JButton("Exit"); // Thêm nút Exit
        stopButton.setBackground(new Color(255, 102, 102));
        webcamButton.setBackground(new Color(0, 153, 255));
        fileButton.setBackground(new Color(150, 123, 255));
        exitButton.setBackground(new Color(255,255,0));
        buttonPanel.add(stopButton);
        buttonPanel.add(webcamButton);
        buttonPanel.add(fileButton);
        buttonPanel.add(exitButton); // Thêm nút Exit vào panel

        JPanel resultPanel = new JPanel();
        JLabel imageLabel = new JLabel("Display Detected Faces Here");
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultPanel.add(imageLabel);

        controlsPanel.add(buttonPanel, BorderLayout.SOUTH);
        controlsPanel.add(resultPanel, BorderLayout.CENTER);

        mainPanel.add(logoPanel, BorderLayout.NORTH);
        mainPanel.add(controlsPanel, BorderLayout.CENTER);

        frame.getContentPane().add(mainPanel);
        frame.setVisible(true);

        webcamButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    runFaceDetection(1, null, imageLabel);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showOpenDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        runFaceDetection(2, selectedFile.getAbsolutePath(), imageLabel);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (capture != null && capture.isOpened()) {
                    capture.release();
                    timer.stop();
                    imageLabel.setIcon(null);
                    JOptionPane.showMessageDialog(frame, "Đã ngừng sử dụng Webcam!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirmed = JOptionPane.showConfirmDialog(frame,
                        "Bạn có chắc chắn muốn thoát khỏi chương trình?",
                        "Confirm Exit", JOptionPane.YES_NO_OPTION);
                if (confirmed == JOptionPane.YES_OPTION) {
                    System.exit(0); // Thoát chương trình
                }
            }
        });
    }

    private static BufferedImage makeCircular(BufferedImage image) {
        int diameter = Math.min(image.getWidth(), image.getHeight());
        BufferedImage mask = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = mask.createGraphics();
        applyQualityRenderingHints(g2);
        g2.fill(new Ellipse2D.Double(0, 0, diameter, diameter));
        g2.dispose();

        BufferedImage circular = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        g2 = circular.createGraphics();
        applyQualityRenderingHints(g2);
        g2.setClip(new Ellipse2D.Double(0, 0, diameter, diameter));
        g2.drawImage(image, 0, 0, diameter, diameter, null);
        g2.dispose();

        return circular;
    }

    private static void applyQualityRenderingHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static void runFaceDetection(int option, String filePath, JLabel imageLabel) throws IOException {
        try (Socket socket = new Socket("localhost", 12345);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String name = JOptionPane.showInputDialog("Enter your name:");
            out.println(name);
            out.println(option);

            if (option == 2 && filePath != null) {
                out.println(filePath);

                BufferedImage image = ImageIO.read(new File(filePath));
                if (image != null) {
                    Mat matImage = bufferedImageToMat(image);

                    CascadeClassifier faceDetector = new CascadeClassifier(CASCADE_PATH);

                    MatOfRect faceDetections = new MatOfRect();
                    faceDetector.detectMultiScale(matImage, faceDetections);

                    for (Rect rect : faceDetections.toArray()) {
                        Imgproc.rectangle(matImage, rect.tl(), rect.br(), new Scalar(0, 255, 0), 3);
                        Imgproc.putText(matImage, name, new Point(rect.x, rect.y - 10), Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, new Scalar(0, 255, 0), 2);
                    }

                    BufferedImage processedImage = matToBufferedImage(matImage);

                    BufferedImage finalImage = resizeImage(processedImage, 500, 600);

                    imageLabel.setIcon(new ImageIcon(finalImage));
                }

                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println(response);
                }
            } else if (option == 1) {
                capture = new VideoCapture(0);
                if (!capture.isOpened()) {
                    System.out.println("Error: Unable to open webcam.");
                    return;
                }

                CascadeClassifier faceDetector = new CascadeClassifier(CASCADE_PATH);
                Mat frame = new Mat();

                timer = new Timer(30, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (capture.read(frame)) {
                            Mat grayFrame = new Mat();
                            Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);

                            MatOfRect faceDetections = new MatOfRect();
                            faceDetector.detectMultiScale(grayFrame, faceDetections);

                            if (faceDetections.toArray().length > 0) {
                                for (Rect rect : faceDetections.toArray()) {
                                    Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(0, 255, 0), 3);
                                    Imgproc.putText(frame, name, new Point(rect.x, rect.y - 10), Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, new Scalar(0, 255, 0), 2);
                                }

                                BufferedImage processedImage = matToBufferedImage(frame);
                                imageLabel.setIcon(new ImageIcon(processedImage));
                            }
                        }
                    }
                });
                timer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resultingImage, 0, 0, null);
        g2d.dispose();
        return outputImage;
    }

    private static Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }

    private static BufferedImage matToBufferedImage(Mat mat) {
        int type = (mat.channels() > 1) ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }
}