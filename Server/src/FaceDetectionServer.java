import javax.swing.*;
import java.awt.*; 
import java.io.*; 
import java.net.ServerSocket; 
import java.net.Socket; 
import java.sql.*; 
import org.opencv.core.*; 
import org.opencv.core.Point; 
import org.opencv.imgcodecs.Imgcodecs; 
import org.opencv.imgproc.Imgproc; 
import org.opencv.objdetect.CascadeClassifier; 
import org.opencv.videoio.VideoCapture; 
import org.opencv.videoio.Videoio; 

public class FaceDetectionServer {
    private static final int WEBCAM_OPTION = 1; // Hằng số cho tùy chọn sử dụng webcam
    private static final int FILE_OPTION = 2; // Hằng số cho tùy chọn sử dụng tệp
    private static final String CASCADE_PATH = "F:\\Demojava\\.vscode\\haarcascade_frontalface_default.xml"; // Đường dẫn tới file cascade để phát hiện khuôn mặt
    private static final String DB_URL = "jdbc:mysql://localhost:3306/face_detection_db"; // URL kết nối tới cơ sở dữ liệu
    private static final String DB_USER = "root"; // Tên người dùng cơ sở dữ liệu
    private static final String DB_PASSWORD = "admin"; // Mật khẩu cơ sở dữ liệu

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Tải thư viện gốc của OpenCV

        try (ServerSocket serverSocket = new ServerSocket(12345)) { // Tạo ServerSocket lắng nghe trên cổng 12345
            System.out.println("Server đang lắng nghe trên cổng 12345");

            while (true) { // Vòng lặp vô hạn để chấp nhận và xử lý các kết nối khách hàng
                Socket socket = serverSocket.accept(); // Chấp nhận kết nối từ khách hàng
                System.out.println("Khách hàng mới kết nối");
                new ClientHandler(socket).start(); // Tạo và bắt đầu một luồng mới để xử lý khách hàng
            }
        } catch (IOException ex) { // Xử lý ngoại lệ IO
            ex.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread { // Lớp con để xử lý các kết nối khách hàng
        private Socket socket; // Socket kết nối với khách hàng

        public ClientHandler(Socket socket) { // Hàm khởi tạo
            this.socket = socket;
        }

        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

                writer.println("Nhập tên của bạn:"); // Gửi yêu cầu nhập tên tới khách hàng
                String name = reader.readLine(); // Đọc tên từ khách hàng

                writer.println("Chọn phương pháp phát hiện khuôn mặt: 1 cho Webcam, 2 cho File"); // Gửi yêu cầu chọn phương pháp phát hiện khuôn mặt
                int choice = Integer.parseInt(reader.readLine()); // Đọc lựa chọn từ khách hàng

                String filePath = null;
                if (choice == WEBCAM_OPTION) {
                    writer.println("Sử dụng Webcam để phát hiện khuôn mặt"); // Thông báo sử dụng webcam
                } else if (choice == FILE_OPTION) {
                    writer.println("Sử dụng Tệp để phát hiện khuôn mặt"); // Thông báo sử dụng tệp
                    writer.println("Vui lòng gửi đường dẫn đến tệp:"); // Yêu cầu đường dẫn tệp
                    filePath = reader.readLine(); // Đọc đường dẫn tệp từ khách hàng
                } else {
                    writer.println("Lựa chọn không hợp lệ"); // Thông báo lựa chọn không hợp lệ
                    return;
                }

                String result = runFaceDetection(choice, filePath, name); // Chạy phát hiện khuôn mặt
                writer.println(result); // Gửi kết quả cho khách hàng
            } catch (IOException ex) { // Xử lý ngoại lệ IO
                ex.printStackTrace();
            }
        }

        private String runFaceDetection(int option, String filePath, String name) {
            CascadeClassifier faceDetector = new CascadeClassifier(CASCADE_PATH); // Tạo bộ phát hiện khuôn mặt
            Mat image = new Mat(); // Tạo đối tượng hình ảnh Mat

            if (option == WEBCAM_OPTION) { // Nếu chọn tùy chọn webcam
                VideoCapture capture = new VideoCapture(0, Videoio.CAP_DSHOW); // Mở webcam
                if (!capture.isOpened()) {
                    return "Không thể mở webcam."; // Thông báo lỗi nếu không mở được webcam
                }

                capture.read(image); // Đọc hình ảnh từ webcam
                capture.release(); // Giải phóng webcam

                if (image.empty()) {
                    return "Không thể lấy hình ảnh từ webcam."; // Thông báo lỗi nếu không lấy được hình ảnh
                }
            } else { // Nếu chọn tùy chọn tệp
                image = Imgcodecs.imread(filePath); // Đọc hình ảnh từ tệp
                if (image.empty()) {
                    return "Không thể đọc hình ảnh từ tệp."; // Thông báo lỗi nếu không đọc được hình ảnh
                }
            }

            Mat grayImage = new Mat(); // Tạo đối tượng hình ảnh xám
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY); // Chuyển đổi hình ảnh sang ảnh xám

            MatOfRect faceDetections = new MatOfRect(); // Tạo đối tượng lưu trữ các vùng phát hiện khuôn mặt
            faceDetector.detectMultiScale(grayImage, faceDetections); // Phát hiện khuôn mặt

            if (faceDetections.toArray().length > 0) { // Nếu phát hiện được khuôn mặt
                for (Rect rect : faceDetections.toArray()) { // Lặp qua các khuôn mặt phát hiện được
                    Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0), 2); // Vẽ hình chữ nhật quanh khuôn mặt
                    Imgproc.putText(image, name, new Point(rect.x, rect.y - 10), Imgproc.FONT_HERSHEY_SIMPLEX, 0.9, new Scalar(0, 255, 0), 2); // Viết tên trên khuôn mặt
                }

                double similarity = checkFaceInDatabase(grayImage); // Kiểm tra sự tương đồng với cơ sở dữ liệu
                if (similarity >= 80.0) {
                    return "Phát hiện khuôn mặt: " + name + " với độ tương đồng: " + similarity + "%"; // Trả về kết quả nếu khuôn mặt tương đồng
                } else {
                    saveImageToDatabase(image, name); // Lưu khuôn mặt mới vào cơ sở dữ liệu
                    return "Khuôn mặt mới được phát hiện và thêm vào cơ sở dữ liệu."; // Thông báo lưu thành công
                }
            } else {
                String message = "Không phát hiện thấy khuôn mặt.";
                showMessage(null, message,"thông báo", JOptionPane.INFORMATION_MESSAGE);
                return  message;
            }
        }

        private double checkFaceInDatabase(Mat newFace) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) { // Kết nối cơ sở dữ liệu
                String sql = "SELECT name, image_data FROM captured_images"; // Câu lệnh SQL để lấy dữ liệu hình ảnh
                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) { // Lặp qua các bản ghi trong cơ sở dữ liệu
                        String dbName = rs.getString("name"); // Lấy tên từ cơ sở dữ liệu
                        byte[] imageData = rs.getBytes("image_data"); // Lấy dữ liệu hình ảnh từ cơ sở dữ liệu
                        Mat referenceFace = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_GRAYSCALE); // Giải mã dữ liệu hình ảnh
                        double similarity = compareFaces(newFace, referenceFace); // So sánh khuôn mặt mới với khuôn mặt trong cơ sở dữ liệu
                        if (similarity >= 80.0) {
                            String message = "Phát hiện khuôn mặt bạn giống với: " + dbName + " với độ tương đồng: " + similarity + "%";
                            showMessage(null, message,"Thông báo", JOptionPane.INFORMATION_MESSAGE); // Thông báo nếu khuôn mặt tương đồng
                            return similarity;
                        }
                    }
                }
            } catch (SQLException e) { // Xử lý ngoại lệ SQL
                e.printStackTrace();
            }
            return 0.0;
        }

        private static double compareFaces(Mat img1, Mat img2) {
            if (img1.empty() || img2.empty()) { // Kiểm tra xem hình ảnh có trống không
                return 0.0;
            }

            Mat resizedImg1 = new Mat(); // Tạo đối tượng hình ảnh đã thay đổi kích thước
            Mat resizedImg2 = new Mat(); // Tạo đối tượng hình ảnh đã thay đổi kích thước
            Size size = new Size(100, 100); // Kích thước thay đổi
            Imgproc.resize(img1, resizedImg1, size); // Thay đổi kích thước hình ảnh
            Imgproc.resize(img2, resizedImg2, size); // Thay đổi kích thước hình ảnh

            Mat diff = new Mat(); // Tạo đối tượng để lưu trữ sự khác biệt
            Core.absdiff(resizedImg1, resizedImg2, diff); // Tính toán sự khác biệt tuyệt đối giữa hai hình ảnh

            double totalDiff = 0.0; // Biến để lưu trữ tổng sự khác biệt
            for (int i = 0; i < diff.rows(); i++) { // Lặp qua các hàng của hình ảnh khác biệt
                for (int j = 0; j < diff.cols(); j++) { // Lặp qua các cột của hình ảnh khác biệt
                    totalDiff += diff.get(i, j)[0]; // Cộng giá trị sự khác biệt vào tổng
                }
            }

            double meanDiff = totalDiff / (size.width * size.height); // Tính toán sự khác biệt trung bình
            
            // Tính toán độ tương đồng. Độ tương đồng được xác định bằng cách lấy 1.0 trừ đi sự khác biệt trung bình chia cho 255 (giá trị lớn nhất của một pixel),
            // đảm bảo giá trị không âm bằng Math.max
            double similarity = Math.max(0.0, 1.0 - meanDiff / 255.0); 

            return similarity * 100.0; // Trả về độ tương đồng dưới dạng phần trăm
        }

        private void saveImageToDatabase(Mat image, String name) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) { // Kết nối cơ sở dữ liệu
                String sql = "INSERT INTO captured_images (name, image_data) VALUES (?, ?)"; // Câu lệnh SQL để chèn dữ liệu hình ảnh mới
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    MatOfByte matOfByte = new MatOfByte(); // Tạo đối tượng MatOfByte để lưu trữ dữ liệu hình ảnh
                    Imgcodecs.imencode(".png", image, matOfByte); // Mã hóa hình ảnh thành PNG
                    byte[] imageData = matOfByte.toArray(); // Chuyển đổi đối tượng MatOfByte thành mảng byte

                    pstmt.setString(1, name); // Đặt tên trong câu lệnh SQL
                    pstmt.setBytes(2, imageData); // Đặt dữ liệu hình ảnh trong câu lệnh SQL
                    pstmt.executeUpdate(); // Thực thi câu lệnh SQL
                    String message = "Hình ảnh đã được lưu vào cơ sở dữ liệu thành công."; // Thông báo lưu thành công
                    showMessage(null, message, "Thông báo", JOptionPane.INFORMATION_MESSAGE); // Hiển thị thông báo
                }
            } catch (SQLException e) { // Xử lý ngoại lệ SQL
                e.printStackTrace();
            }
        }

        // Phương thức đặt thông báo lên trên cùng, đè lên tất cả các layer khác
        private static void showMessage(Component parent, String message, String title, int informationMessage) {
            JDialog dialog = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION).createDialog(parent, title); // Tạo hộp thoại thông báo
            dialog.setAlwaysOnTop(true); // Đặt thuộc tính luôn hiển thị trên cùng
            dialog.setVisible(true); // Hiển thị hộp thoại
        }
    }
}
