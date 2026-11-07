package com.giapha.view;

import com.giapha.model.Gender;
import com.giapha.model.Person;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Sơ đồ cây gia phả 2D — vẽ bằng {@link Pane} + {@link Line}.
 *
 * <p><b>Bố cục:</b>
 * <ul>
 *     <li>Mỗi thế hệ nằm trên một hàng; tổ tiên ở TRÊN, hậu duệ ở DƯỚI người trung tâm.</li>
 *     <li>Mỗi node hiển thị avatar + họ tên + năm sinh + giới tính.</li>
 *     <li>Đường kẻ nối cha/mẹ xuống con. Vợ/chồng được gom thành một "khối gia đình"
 *         (couple unit) để hai người luôn nằm cạnh nhau và con của họ nằm chính giữa.</li>
 * </ul>
 *
 * <p><b>Tương tác:</b>
 * <ul>
 *     <li>Kéo chuột trái để di chuyển (pan).</li>
 *     <li>Lăn chuột dọc để cuộn, Shift + lăn chuột để cuộn ngang.</li>
 *     <li>Ctrl + lăn chuột để zoom.</li>
 *     <li>Bấm vào một node để mở hồ sơ cá nhân.</li>
 * </ul>
 *
 * <p>Không dùng {@code ScrollPane} vì {@code viewportTransform} của ScrollPane không
 * tương thích tốt với việc pan + zoom đồng thời; thay vào đó component tự quản lý
 * {@code translate} và {@code scale} trên một lớp nội dung riêng.
 */
public class FamilyTreeDiagram extends Pane {

    /** Chiều rộng một node (px, ở mức zoom 1.0). */
    private static final double NODE_WIDTH = 150;

    /** Chiều cao một node (px, ở mức zoom 1.0). */
    private static final double NODE_HEIGHT = 72;

    /** Khoảng cách ngang giữa hai cột (px). */
    private static final double H_GAP = 22;

    /** Khoảng cách dọc giữa hai hàng thế hệ (px). */
    private static final double V_GAP = 62;

    /** Lề an toàn quanh toàn bộ sơ đồ (px). */
    private static final double MARGIN = 40;

    /** Khoảng cách tối thiểu giữa hai khối gia đình cùng hàng (đơn vị cột). */
    private static final double UNIT_GAP = 0.8;

    /** Mức zoom nhỏ nhất. */
    private static final double MIN_ZOOM = 0.25;

    /** Mức zoom lớn nhất. */
    private static final double MAX_ZOOM = 2.5;

    /** Lớp chứa toàn bộ node + đường kẻ, được transform để zoom/pan. */
    private final Pane content = new Pane();

    /** Kích thước nội dung ở mức zoom 1.0 (dùng để tính vùng cuộn). */
    private double baseWidth = 0;
    private double baseHeight = 0;

    /** Danh sách toàn bộ thành viên — nguồn dữ liệu để tìm quan hệ cha/mẹ/con. */
    private List<Person> allPersons = List.of();

    /** Người trung tâm của sơ đồ. */
    private Person focusPerson;

    /** Callback khi người dùng bấm vào một node. */
    private Consumer<Person> onViewPerson;

    /** Vị trí kéo chuột đang di chuyển. */
    private double dragAnchorX;
    private double dragAnchorY;
    private double dragOriginX;
    private double dragOriginY;

    /** Cờ yêu cầu tự động canh sơ đồ vào khung nhìn khi có kích thước thực. */
    private boolean needsFit = false;

    /**
     * Thuộc tính theo dõi mức zoom hiện tại — dùng để đồng bộ nhãn % trên toolbar.
     */
    private final javafx.beans.property.ReadOnlyDoubleWrapper zoomProperty =
            new javafx.beans.property.ReadOnlyDoubleWrapper(this, "zoom", 1.0);

    public FamilyTreeDiagram() {
        content.setManaged(false);
        getChildren().add(content);
        getStyleClass().add("family-tree-diagram");

        clipRect.widthProperty().bind(widthProperty());
        clipRect.heightProperty().bind(heightProperty());
        setClip(clipRect);

        // Khi component có kích thước thực, canh sơ đồ vào khung nhìn
        widthProperty().addListener((obs, oldVal, newVal) -> tryFit());
        heightProperty().addListener((obs, oldVal, newVal) -> tryFit());

        // Kéo chuột để pan
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragAnchorX = e.getX();
                dragAnchorY = e.getY();
                dragOriginX = content.getTranslateX();
                dragOriginY = content.getTranslateY();
            }
        });
        setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown()) {
                content.setTranslateX(dragOriginX + (e.getX() - dragAnchorX));
                content.setTranslateY(dragOriginY + (e.getY() - dragAnchorY));
            }
        });

        // Lăn chuột: cuộn dọc / ngang, Ctrl để zoom
        setOnScroll(e -> {
            if (e.isControlDown()) {
                double factor = e.getDeltaY() > 0 ? 1.1 : 1 / 1.1;
                zoomAround(e.getX(), e.getY(), factor);
            } else if (e.isShiftDown()) {
                content.setTranslateX(content.getTranslateX() + e.getDeltaY());
            } else {
                content.setTranslateY(content.getTranslateY() + e.getDeltaY());
            }
            e.consume();
        });
    }

    /** Vùng cắt (clip) để nội dung không tràn ra ngoài khung nhìn. */
    private final Rectangle clipRect = new Rectangle();

    /**
     * Thuộc tính chỉ đọc chứa mức zoom hiện tại.
     *
     * @return property zoom
     */
    public javafx.beans.property.ReadOnlyDoubleProperty contentScaleProperty() {
        return zoomProperty.getReadOnlyProperty();
    }

    /**
     * Nạp dữ liệu và dựng lại sơ đồ.
     *
     * @param allPersons  toàn bộ thành viên (đồ thị liên kết đầy đủ)
     * @param focusPerson người trung tâm
     */
    public void setData(List<Person> allPersons, Person focusPerson) {
        this.allPersons = allPersons != null ? allPersons : List.of();
        this.focusPerson = focusPerson;
        rebuild();
    }

    // ============================================================
    // DỰNG SƠ ĐỒ
    // ============================================================

    /**
     * Dựng lại toàn bộ sơ đồ từ đầu.
     */
    private void rebuild() {
        content.getChildren().clear();
        baseWidth = 0;
        baseHeight = 0;

        if (focusPerson == null || allPersons.isEmpty()) {
            return;
        }

        Map<Integer, DNode> nodes = buildModel();
        if (nodes.isEmpty()) {
            return;
        }

        Map<Integer, Unit> units = buildUnits(nodes);
        Unit focusUnit = unitOf(units, nodes.get(focusPerson.getId()));

        Set<Integer> ancestorSet = collectAncestorUnits(units, focusUnit);
        Set<Integer> descendantSet = collectDescendantUnits(units, focusUnit);

        layoutAll(units, focusUnit, ancestorSet, descendantSet);

        assignPixelPositions(nodes, units);

        // Vẽ đường kẻ trước, node sau (để node nằm đè lên đường kẻ)
        List<javafx.scene.Node> edges = new ArrayList<>();
        List<javafx.scene.Node> nodeCards = new ArrayList<>();
        drawEdges(units, edges);
        for (DNode n : nodes.values()) {
            nodeCards.add(createNodeCard(n));
        }

        content.getChildren().addAll(edges);
        content.getChildren().addAll(nodeCards);

        content.setPrefSize(baseWidth, baseHeight);
        content.setMinSize(baseWidth, baseHeight);
        content.setMaxSize(baseWidth, baseHeight);

        // Nếu chưa có kích thước thực (view chưa hiển thị), đánh dấu để fit lại sau
        if (getWidth() > 0 && getHeight() > 0) {
            fitToView();
        } else {
            needsFit = true;
        }
    }

    /**
     * Tự động canh sơ đồ vào khung nhìn khi component có kích thước thực.
     */
    private void tryFit() {
        if (needsFit && getWidth() > 0 && getHeight() > 0 && baseWidth > 0) {
            needsFit = false;
            fitToView();
        }
    }

    // ============================================================
    // MÔ HÌNH ĐỒ THỊ
    // ============================================================

    /**
     * Dựng tập node hiển thị: người trung tâm, toàn bộ tổ tiên, toàn bộ hậu duệ
     * và cả vợ/chồng của các người trong tập đó (để cây không bị thiếu "nửa gia đình").
     *
     * @return map personId → node
     */
    private Map<Integer, DNode> buildModel() {
        Map<Integer, DNode> byId = new LinkedHashMap<>();
        DNode focus = new DNode(focusPerson, 0);
        byId.put(focusPerson.getId(), focus);

        Deque<DNode> queue = new ArrayDeque<>();
        Set<Integer> expanded = new HashSet<>();
        queue.add(focus);

        while (!queue.isEmpty()) {
            DNode current = queue.removeFirst();

            // --- Đi lên: cha/mẹ ---
            for (Person parent : parentsOf(current.person)) {
                DNode pn = byId.computeIfAbsent(parent.getId(),
                        id -> new DNode(parent, current.generation - 1));
                if (!pn.below.contains(current)) {
                    pn.below.add(current);
                }
                if (expanded.add(parent.getId())) {
                    queue.addLast(pn);
                }
            }

            // --- Đi xuống: con ---
            for (Person child : childrenOf(current.person)) {
                DNode cn = byId.computeIfAbsent(child.getId(),
                        id -> new DNode(child, current.generation + 1));
                if (!current.below.contains(cn)) {
                    current.below.add(cn);
                }
                if (expanded.add(child.getId())) {
                    queue.addLast(cn);
                }

                // Vợ/chồng của người trung tâm trong cặp cha–mẹ: cùng thế hệ với current
                Person spouse = otherParent(child, current.person);
                if (spouse != null) {
                    DNode sn = byId.computeIfAbsent(spouse.getId(),
                            id -> new DNode(spouse, current.generation));
                    if (!sn.below.contains(cn)) {
                        sn.below.add(cn);
                    }
                    if (expanded.add(spouse.getId())) {
                        queue.addLast(sn);
                    }
                }
            }
        }

        return byId;
    }

    /**
     * Gom các node thành "khối gia đình" (unit): cha + mẹ cùng sinh chung một con
     * được gom vào một unit để hiển thị cạnh nhau.
     *
     * @return map unitId → unit
     */
    private Map<Integer, Unit> buildUnits(Map<Integer, DNode> nodes) {
        Map<Integer, Unit> units = new LinkedHashMap<>();
        Map<Integer, Integer> unitOfPerson = new HashMap<>();
        int[] nextId = {0};

        // 1. Khối vợ/chồng
        for (DNode n : nodes.values()) {
            Person father = n.person.getFather();
            Person mother = n.person.getMother();
            if (father == null || mother == null) continue;
            if (!nodes.containsKey(father.getId()) || !nodes.containsKey(mother.getId())) continue;
            if (unitOfPerson.containsKey(father.getId()) || unitOfPerson.containsKey(mother.getId())) continue;

            Unit u = new Unit(nextId[0]++);
            u.members.add(nodes.get(father.getId()));
            u.members.add(nodes.get(mother.getId()));
            units.put(u.id, u);
            unitOfPerson.put(father.getId(), u.id);
            unitOfPerson.put(mother.getId(), u.id);
        }

        // 2. Node độc lập
        for (DNode n : nodes.values()) {
            if (unitOfPerson.containsKey(n.person.getId())) continue;
            Unit u = new Unit(nextId[0]++);
            u.members.add(n);
            units.put(u.id, u);
            unitOfPerson.put(n.person.getId(), u.id);
        }

        // 3. Liên kết trên/dưới giữa các unit
        for (Unit u : units.values()) {
            for (DNode m : u.members) {
                for (Person parent : parentsOf(m.person)) {
                    Integer pid = unitOfPerson.get(parent.getId());
                    if (pid == null) continue;
                    Unit pu = units.get(pid);
                    if (pu != u && !u.above.contains(pu)) {
                        u.above.add(pu);
                    }
                }
                for (Person child : childrenOf(m.person)) {
                    Integer cid = unitOfPerson.get(child.getId());
                    if (cid == null) continue;
                    Unit cu = units.get(cid);
                    if (cu != u && !u.below.contains(cu)) {
                        u.below.add(cu);
                    }
                }
            }
        }

        return units;
    }

    /**
     * Lấy unit chứa một node.
     */
    private Unit unitOf(Map<Integer, Unit> units, DNode node) {
        if (node == null) return null;
        for (Unit u : units.values()) {
            if (u.members.contains(node)) {
                return u;
            }
        }
        return null;
    }

    // ============================================================
    // BỐ TRÍ (LAYOUT)
    // ============================================================

    /**
     * Thu thập các unit thuộc nhánh tổ tiên (phía trên người trung tâm).
     */
    private Set<Integer> collectAncestorUnits(Map<Integer, Unit> units, Unit focusUnit) {
        Set<Integer> result = new LinkedHashSet<>();
        if (focusUnit == null) return result;

        Deque<Unit> stack = new ArrayDeque<>();
        stack.push(focusUnit);
        while (!stack.isEmpty()) {
            Unit u = stack.pop();
            for (Unit above : u.above) {
                if (result.add(above.id)) {
                    stack.push(above);
                }
            }
        }
        return result;
    }

    /**
     * Thu thập các unit thuộc nhánh hậu duệ (phía dưới người trung tâm).
     */
    private Set<Integer> collectDescendantUnits(Map<Integer, Unit> units, Unit focusUnit) {
        Set<Integer> result = new LinkedHashSet<>();
        if (focusUnit == null) return result;

        Deque<Unit> stack = new ArrayDeque<>();
        stack.push(focusUnit);
        while (!stack.isEmpty()) {
            Unit u = stack.pop();
            for (Unit below : u.below) {
                if (result.add(below.id)) {
                    stack.push(below);
                }
            }
        }
        return result;
    }

    /**
     * Bố trí toàn bộ sơ đồ.
     *
     * <p>Cách làm:
     * <ol>
     *     <li>Nhánh hậu duệ được bố trí theo thuật toán "cha canh giữa trên tập con"
     *         (top-down), bắt đầu từ khối gia đình chứa người trung tâm.</li>
     *     <li>Nhánh tổ tiên cũng dùng cùng thuật toán nhưng gốc là các khối gia đình
     *         trên cùng; sau đó cả nhánh được dịch để khối ngay trên người trung tâm
     *         thẳng hàng với người trung tâm.</li>
     * </ol>
     */
    private void layoutAll(Map<Integer, Unit> units, Unit focusUnit,
                           Set<Integer> ancestorIds, Set<Integer> descendantIds) {
        double[] counter = {0.0};

        // --- Nhánh hậu duệ ---
        if (focusUnit != null) {
            layoutTree(focusUnit, descendantIds, counter);
        }

        // --- Nhánh tổ tiên ---
        List<Unit> roots = new ArrayList<>();
        for (Unit u : units.values()) {
            if (!ancestorIds.contains(u.id)) continue;
            boolean hasAncestorParent = false;
            for (Unit above : u.above) {
                if (ancestorIds.contains(above.id)) {
                    hasAncestorParent = true;
                    break;
                }
            }
            if (!hasAncestorParent) {
                roots.add(u);
            }
        }
        roots.sort((a, b) -> Integer.compare(a.generation(), b.generation()));
        for (Unit r : roots) {
            layoutTree(r, ancestorIds, counter);
        }

        // Dịch nhánh tổ tiên để khối ngay trên người trung tâm thẳng hàng
        if (focusUnit != null) {
            Unit anchor = null;
            for (Unit u : units.values()) {
                if (ancestorIds.contains(u.id) && u.below.contains(focusUnit)) {
                    anchor = u;
                    break;
                }
            }
            if (anchor != null) {
                double shift = focusUnit.start - anchor.start;
                if (Math.abs(shift) > 0.001) {
                    for (Unit u : units.values()) {
                        if (ancestorIds.contains(u.id)) {
                            u.start += shift;
                        }
                    }
                }
            }
        }
    }

    /**
     * Bố trí một cây theo thuật toán "cha canh giữa trên tập con".
     *
     * <p>Các lá được cấp cột liên tiếp; node cha nhận cột trung bình của
     * lá đầu tiên và lá cuối cùng.
     *
     * @param node      gốc của cây con
     * @param allowed   tập id unit thuộc cây này
     * @param counter   bộ đếm cột (chia sẻ giữa các cây)
     */
    private void layoutTree(Unit node, Set<Integer> allowed, double[] counter) {
        List<Unit> kids = new ArrayList<>();
        for (Unit c : node.below) {
            if (allowed.contains(c.id)) {
                kids.add(c);
            }
        }

        if (kids.isEmpty()) {
            node.start = counter[0];
            counter[0] += node.size();
            return;
        }

        double first = 0;
        double last = 0;
        for (int i = 0; i < kids.size(); i++) {
            layoutTree(kids.get(i), allowed, counter);
            if (i == 0) first = kids.get(i).start;
            last = kids.get(i).start + kids.get(i).size() - 1;
        }
        node.start = (first + last) / 2.0 - (node.size() - 1) / 2.0;
    }

    /**
     * Chuyển toạ độ cột/hàng thành toạ độ pixel, đồng thời tính kích thước nội dung.
     */
    private void assignPixelPositions(Map<Integer, DNode> nodes, Map<Integer, Unit> units) {
        double columnWidth = NODE_WIDTH + H_GAP;
        double rowHeight = NODE_HEIGHT + V_GAP;

        int minGen = Integer.MAX_VALUE;
        int maxGen = Integer.MIN_VALUE;
        for (DNode n : nodes.values()) {
            minGen = Math.min(minGen, n.generation);
            maxGen = Math.max(maxGen, n.generation);
        }

        // Tìm toạ độ cột nhỏ nhất / lớn nhất trên toàn bộ sơ đồ
        double minColumn = Double.MAX_VALUE;
        double maxColumn = -Double.MAX_VALUE;
        for (Unit u : units.values()) {
            minColumn = Math.min(minColumn, u.start);
            maxColumn = Math.max(maxColumn, u.start + u.size() - 1);
        }
        if (minColumn == Double.MAX_VALUE) {
            minColumn = 0;
            maxColumn = 0;
        }

        double offset = -minColumn * columnWidth + MARGIN;
        for (DNode n : nodes.values()) {
            Unit u = unitOf(units, n);
            double column = (u != null ? u.start + u.members.indexOf(n) : 0);
            n.pixelX = column * columnWidth + offset;
            n.pixelY = (n.generation - minGen) * rowHeight + MARGIN;
        }

        baseWidth = (maxColumn - minColumn) * columnWidth + NODE_WIDTH + 2 * MARGIN;
        baseHeight = (maxGen - minGen) * rowHeight + NODE_HEIGHT + 2 * MARGIN;
    }

    // ============================================================
    // VẼ ĐỒ HỌA
    // ============================================================

    /**
     * Vẽ đường kẻ nối cha/mẹ xuống con.
     *
     * <p>Đường kẻ được vẽ theo mức cá nhân (mỗi cha/mẹ nối xuống đúng con của mình),
     * không phải theo khối gia đình — tránh nối nhầm vợ/chồng của người trung tâm
     * với cha/mẹ của họ.
     */
    private void drawEdges(Map<Integer, Unit> units, List<javafx.scene.Node> edges) {
        Map<Integer, DNode> byId = new HashMap<>();
        for (DNode n : nodesOf(units).values()) {
            byId.put(n.person.getId(), n);
        }

        for (DNode parent : byId.values()) {
            for (Person child : childrenOf(parent.person)) {
                DNode childNode = byId.get(child.getId());
                if (childNode == null) continue;
                edges.addAll(createEdges(parent, childNode));
            }
        }
    }

    /**
     * Thu thập toàn bộ node của sơ đồ.
     */
    private Map<Integer, DNode> nodesOf(Map<Integer, Unit> units) {
        Map<Integer, DNode> result = new LinkedHashMap<>();
        for (Unit u : units.values()) {
            for (DNode m : u.members) {
                result.put(m.person.getId(), m);
            }
        }
        return result;
    }

    /**
     * Tạo đường gấp khúc nối một cha/mẹ (phía trên) xuống một con (phía dưới).
     */
    private List<Line> createEdges(DNode parent, DNode child) {
        List<Line> lines = new ArrayList<>();

        double parentCenterX = parent.pixelX + NODE_WIDTH / 2;
        double childCenterX = child.pixelX + NODE_WIDTH / 2;

        double startY = parent.pixelY + NODE_HEIGHT;
        double endY = child.pixelY;
        double middleY = (startY + endY) / 2;

        lines.add(makeLine(parentCenterX, startY, parentCenterX, middleY));
        if (Math.abs(childCenterX - parentCenterX) > 0.5) {
            lines.add(makeLine(parentCenterX, middleY, childCenterX, middleY));
        }
        lines.add(makeLine(childCenterX, middleY, childCenterX, endY));

        return lines;
    }

    /**
     * Tạo một đường thẳng có style mặc định của sơ đồ.
     */
    private Line makeLine(double x1, double y1, double x2, double y2) {
        Line line = new Line(x1, y1, x2, y2);
        line.getStyleClass().add("tree2d-edge");
        return line;
    }

    /**
     * Tạo node hiển thị: avatar + họ tên + năm sinh + giới tính.
     */
    private Pane createNodeCard(DNode node) {
        Person person = node.person;

        VBox card = new VBox(6);
        card.setPrefWidth(NODE_WIDTH);
        card.setPrefHeight(NODE_HEIGHT);
        card.getStyleClass().add("tree2d-node");

        boolean isFocus = person.getId() == focusPerson.getId();
        if (isFocus) {
            card.getStyleClass().add("tree2d-node-focus");
        } else if (node.generation < 0) {
            card.getStyleClass().add("tree2d-node-ancestor");
        } else {
            card.getStyleClass().add("tree2d-node-descendant");
        }

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        top.getChildren().add(new PersonAvatar(person, 38));

        VBox text = new VBox(2);
        Label name = new Label(person.getFullName());
        name.getStyleClass().add("tree2d-name");
        name.setWrapText(true);

        String genderText = person.getGender().getDisplayName();
        Label meta = new Label("Sinh " + person.getBirthYear() + " · " + genderText);
        meta.getStyleClass().add("tree2d-meta");

        text.getChildren().addAll(name, meta);
        top.getChildren().add(text);

        card.getChildren().add(top);

        card.setLayoutX(node.pixelX);
        card.setLayoutY(node.pixelY);

        card.setOnMouseClicked(e -> {
            if (onViewPerson != null) {
                onViewPerson.accept(person);
            }
        });

        return card;
    }

    // ============================================================
    // QUẢN LÝ TÍNH DỊCH VÀ ZOOM
    // ============================================================

    /**
     * Zoom quanh một điểm (giữ nguyên vị trí con trỏ).
     *
     * @param localX toạ độ X theo hệ của chính component
     * @param localY toạ độ Y theo hệ của chính component
     * @param factor hệ số nhân mức zoom
     */
    private void zoomAround(double localX, double localY, double factor) {
        double oldZoom = content.getScaleX();
        double newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, oldZoom * factor));
        if (newZoom == oldZoom) {
            return;
        }

        double contentX = (localX - content.getTranslateX()) / oldZoom;
        double contentY = (localY - content.getTranslateY()) / oldZoom;

        content.setScaleX(newZoom);
        content.setScaleY(newZoom);
        content.setTranslateX(localX - contentX * newZoom);
        content.setTranslateY(localY - contentY * newZoom);
        zoomProperty.set(newZoom);
    }

    /**
     * Nhân mức zoom hiện tại với một hệ số, lấy tâm làm gốc.
     *
     * @param factor hệ số nhân (&gt;1 phóng to, &lt;1 thu nhỏ)
     */
    public void zoomBy(double factor) {
        double centerX = getWidth() / 2;
        double centerY = getHeight() / 2;
        zoomAround(centerX, centerY, factor);
    }

    /**
     * Thu về mức zoom 1.0 và canh giữa sơ đồ trong khung nhìn.
     */
    public void resetZoom() {
        content.setScaleX(1.0);
        content.setScaleY(1.0);
        content.setTranslateX(0);
        content.setTranslateY(0);
        zoomProperty.set(1.0);
        fitToView();
    }

    /**
     * Tự động chọn mức zoom để toàn bộ sơ đồ vừa khung nhìn.
     */
    public void fitToView() {
        if (baseWidth <= 0 || baseHeight <= 0 || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        double scaleX = (getWidth() - 20) / baseWidth;
        double scaleY = (getHeight() - 20) / baseHeight;
        double zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Math.min(scaleX, scaleY)));

        content.setScaleX(zoom);
        content.setScaleY(zoom);
        content.setTranslateX((getWidth() - baseWidth * zoom) / 2);
        content.setTranslateY((getHeight() - baseHeight * zoom) / 2);
        zoomProperty.set(zoom);
    }

    /**
     * Chụp toàn bộ sơ đồ (kể cả phần đang bị cuộn/zoom ra ngoài khung nhìn)
     * thành một ảnh PNG ở mức zoom 1.0.
     *
     * <p>Hàm tạm thời đặt lại transform của lớp nội dung về zoom 1.0, chụp ảnh
     * với viewport đúng bằng kích thước nội dung, rồi khôi phục lại trạng thái
     * cũ để người dùng không bị mất góc nhìn hiện tại.
     *
     * @return ảnh chụp toàn bộ sơ đồ
     */
    public WritableImage renderFullImage() {
        if (baseWidth <= 0 || baseHeight <= 0) {
            return null;
        }

        double oldScaleX = content.getScaleX();
        double oldScaleY = content.getScaleY();
        double oldTranslateX = content.getTranslateX();
        double oldTranslateY = content.getTranslateY();

        content.setScaleX(1.0);
        content.setScaleY(1.0);
        content.setTranslateX(0);
        content.setTranslateY(0);

        WritableImage image;
        try {
            SnapshotParameters params = new SnapshotParameters();
            params.setViewport(new Rectangle2D(0, 0, baseWidth, baseHeight));
            params.setFill(Color.WHITE);
            image = content.snapshot(params, null);
        } finally {
            content.setScaleX(oldScaleX);
            content.setScaleY(oldScaleY);
            content.setTranslateX(oldTranslateX);
            content.setTranslateY(oldTranslateY);
        }
        return image;
    }

    /**
     * Đặt callback khi người dùng bấm vào một node.
     *
     * @param handler callback nhận Person được chọn
     */
    public void setOnViewPerson(Consumer<Person> handler) {
        this.onViewPerson = handler;
    }

    // ============================================================
    // TRUY VẤN QUAN HỆ
    // ============================================================

    /**
     * Lấy cha và mẹ của một người (bỏ qua null — không dùng List.of vì không cho phép null).
     */
    private List<Person> parentsOf(Person person) {
        List<Person> parents = new ArrayList<>(2);
        if (person.getFather() != null) parents.add(person.getFather());
        if (person.getMother() != null) parents.add(person.getMother());
        return parents;
    }

    /**
     * Lấy danh sách con của một người (theo cả cha và mẹ).
     */
    private List<Person> childrenOf(Person person) {
        List<Person> children = new ArrayList<>();
        for (Person p : allPersons) {
            boolean isFather = p.getFather() != null && p.getFather().getId() == person.getId();
            boolean isMother = p.getMother() != null && p.getMother().getId() == person.getId();
            if (isFather || isMother) {
                children.add(p);
            }
        }
        return children;
    }

    /**
     * Tìm vợ/chồng của một người trong cặp cha–mẹ của một đứa trẻ.
     *
     * @param child  đứa trẻ
     * @param person một trong hai người làm cha/mẹ
     * @return người còn lại trong cặp, hoặc null nếu không xác định được
     */
    private Person otherParent(Person child, Person person) {
        Person father = child.getFather();
        Person mother = child.getMother();
        if (father == null || mother == null) return null;
        if (father.getId() == person.getId()) return mother;
        if (mother.getId() == person.getId()) return father;
        return null;
    }

    // ============================================================
    // MÔ HÌNH NỘI BỘ
    // ============================================================

    /**
     * Node trong mô hình đồ thị hiển thị.
     */
    private static final class DNode {
        private final Person person;

        /** Thế hệ: âm = tổ tiên, 0 = người trung tâm, dương = hậu duệ. */
        private final int generation;

        /** Các node nằm ngay bên dưới node này. */
        private final List<DNode> below = new ArrayList<>();

        /** Toạ độ pixel (góc trên trái). */
        private double pixelX;
        private double pixelY;

        private DNode(Person person, int generation) {
            this.person = person;
            this.generation = generation;
        }
    }

    /**
     * Khối gia đình — gom 1 hoặc 2 người (cha + mẹ) để hiển thị cạnh nhau.
     */
    private static final class Unit {
        private final int id;
        private final List<DNode> members = new ArrayList<>();

        /** Các unit nằm phía trên (cha/mẹ). */
        private final List<Unit> above = new ArrayList<>();

        /** Các unit nằm phía dưới (con). */
        private final List<Unit> below = new ArrayList<>();

        /** Toạ độ cột đầu tiên của unit. */
        private double start = 0;

        private Unit(int id) {
            this.id = id;
        }

        /** Số thành viên (số cột unit chiếm). */
        private int size() {
            return Math.max(1, members.size());
        }

        /** Thế hệ của unit (lấy theo thành viên đầu tiên). */
        private int generation() {
            return members.isEmpty() ? 0 : members.get(0).generation;
        }
    }
}
