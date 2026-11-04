package com.giapha.support;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tiện ích khởi tạo & quản lý JavaFX Toolkit cho các lớp kiểm thử cần giao diện.
 *
 * <p><b>Quy trắc quan trọng:</b> ảnh trong JavaFX được nạp ở chế độ background.
 * Vì vậy KHÔNG được {@code sleep} trên JavaFX Application Thread — sẽ chặn việc
 * nạp ảnh. Test dựng toàn bộ UI trên FX thread, thoát khỏi FX thread, rồi chờ
 * nạp xong ở thread chính mới kiểm tra kết quả.
 *
 * <p>Các test này được gắn tag {@code javafx} và <b>KHÔNG</b> chạy trong
 * {@code mvn test} mặc định (xem README — mục "Kiểm thử").
 */
public final class FxTestSupport {

    /** Timeout tối đa cho một tác vụ trên FX thread (giây). */
    public static final int FX_TASK_TIMEOUT_SECONDS = 15;

    /** Stage giả, dùng để giữ JavaFX Toolkit sống suốt phiên kiểm thử. */
    private static Stage dummyStage;

    private static boolean toolkitStarted;

    static {
        // Đóng toolkit khi JVM kết thúc — JavaFX không thể khởi tạo lại
        // sau khi Platform.exit() đã được gọi.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Platform.exit();
            } catch (Throwable ignored) {
                // bỏ qua
            }
        }));
    }

    private FxTestSupport() {
        // Utility class
    }

    /**
     * Khởi tạo JavaFX Toolkit (idempotent — gọi nhiều lần cũng được).
     *
     * <p>Tắt {@code Platform.setImplicitExit(false)} để toolkit không tự thoát
     * khi Stage giả bị đóng, và tự động đóng mọi hộp thoại Alert/Dialog để
     * không làm treo luồng kiểm thử tự động.
     */
    public static synchronized void initToolkit() {
        if (toolkitStarted) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                dummyStage = new Stage();
                dummyStage.setTitle("FxTestSupport Dummy Stage");
                setupDialogAutoDismiss();
                latch.countDown();
            });
        } catch (IllegalStateException e) {
            // Toolkit đã được khởi tạo trước đó (ví dụ do test khác)
            setupDialogAutoDismiss();
            latch.countDown();
        }

        try {
            if (!latch.await(FX_TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                System.err.println("[WARN] Timeout khởi tạo JavaFX Toolkit");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        toolkitStarted = true;
    }

    /**
     * Đóng JavaFX Toolkit khi kết thúc phiên kiểm thử.
     *
     * <p><b>Lưu ý:</b> JavaFX Toolkit KHÔNG thể khởi tạo lại sau khi
     * {@code Platform.exit()} được gọi. Vì vậy phương thức này chỉ đóng
     * Stage giả và đăng ký một shutdown hook để {@code Platform.exit()}
     * được gọi đúng lúc JVM kết thúc — để nhiều lớp test JavaFX có thể
     * dùng chung một Toolkit trong cùng một phiên surefire.
     */
    public static synchronized void shutdownToolkit() {
        if (!toolkitStarted) {
            return;
        }
        try {
            runOnFxThread(() -> {
                if (dummyStage != null) {
                    dummyStage.close();
                }
            });
        } catch (Exception ignored) {
            // Dọn dẹp là thao tác phụ trợ
        }
        toolkitStarted = false;
    }

    /**
     * Tự động đóng các cửa sổ hộp thoại Alert / Dialog khi hiển thị,
     * ngăn chặn phương thức {@code showAndWait()} làm treo luồng test tự động.
     */
    private static void setupDialogAutoDismiss() {
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Window w : change.getAddedSubList()) {
                        if (w instanceof Stage s && w != dummyStage) {
                            Platform.runLater(() -> {
                                try {
                                    s.close();
                                } catch (Exception ignored) {
                                    // bỏ qua
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    /** Tác vụ chạy trên JavaFX Application Thread. */
    @FunctionalInterface
    public interface FxAction {
        void execute() throws Exception;
    }

    /**
     * Chạy một tác vụ trên JavaFX Application Thread và chờ hoàn tất.
     *
     * @param action tác vụ cần thực thi
     * @throws Exception nếu tác vụ ném exception hoặc hết thời gian chờ
     */
    public static void runOnFxThread(FxAction action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            action.execute();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                action.execute();
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(FX_TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            throw new IllegalStateException(
                    "JavaFX Application Thread action timed out sau " + FX_TASK_TIMEOUT_SECONDS + " giây");
        }
        if (errorRef.get() != null) {
            if (errorRef.get() instanceof Exception ex) {
                throw ex;
            }
            throw new IllegalStateException(errorRef.get());
        }
    }
}
