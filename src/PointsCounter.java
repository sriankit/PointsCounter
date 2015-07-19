import javax.swing.*;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class PointsCounter extends Applet implements MouseListener, MouseMotionListener {
    ArrayList<Point> points = new ArrayList<Point>();
    Rectangle motionRect;
    boolean moreInput = true;
    Point last, frst;
    int stx, sty;
    Mode mode = Mode.IDLE;
    int cnt;
    JLabel cntShower;
    String s1 = "Points inside Rectangle = ";
    SegTree tree;

    public PointsCounter() {
        setSize(900, 600);
        motionRect = new Rectangle(10, 10, 60, 60);

        addMouseListener(this);
        addMouseMotionListener(this);
        setBackground(Color.BLACK);
        cnt = 0;
        cntShower = new JLabel(s1 + cnt);
        cntShower.setForeground(Color.WHITE);
        add(cntShower);
        JButton doneButton = new JButton("Done");
        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moreInput = false;
                Data[] ps = new Data[points.size()];
                int maxY = 0;
                for (int i = 0; i < points.size(); i++) {
                    ps[i] = new Data(points.get(i).x, points.get(i).y);
                    maxY = Math.max(maxY, points.get(i).y);
                }
                tree = new SegTree(ps, maxY + 1);
            }
        });
        add(doneButton);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        last = e.getPoint();
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point p = e.getPoint();
        Rectangle ryt = new Rectangle((int) motionRect.getMaxX(), (int) motionRect.getMinY(), 10, motionRect.height);
        Rectangle bottom = new Rectangle((int) motionRect.getMinX(), (int) motionRect.getMaxY(), motionRect.width, 10);
        Rectangle both = new Rectangle((int) motionRect.getMaxX(), (int) motionRect.getMaxY(), 10, 10);

        if (motionRect.contains(p)) {
            //mode = Mode.TRANSLATE;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (ryt.contains(p)) {
            //mode = Mode.RIGHT_EXTEND;
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        } else if (bottom.contains(p)) {
            //mode = Mode.BOTTOM_EXTEND;
            setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
        } else if (both.contains(p)) {
            //mode = Mode.BOTH_EXTEND;
            setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
        } else setCursor(Cursor.getDefaultCursor());


    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);    //To change body of overridden methods use File | Settings | File Templates.
        g.setColor(Color.YELLOW);
        for (Point p : points) g.drawOval(p.x, p.y, 5, 5);
        g.setColor(Color.BLUE);
        switch (mode) {
            case TRANSLATE:
                motionRect.setRect(stx + last.x - frst.x, sty + last.y - frst.y, motionRect.width, motionRect.height);
                break;
            case RIGHT_EXTEND:
                motionRect.setRect(motionRect.x, motionRect.y, last.x - motionRect.x, motionRect.height);
                break;
            case BOTTOM_EXTEND:
                motionRect.setRect(motionRect.x, motionRect.y, motionRect.width, last.y - motionRect.y);
                break;
            case BOTH_EXTEND:
                motionRect.setRect(motionRect.x, motionRect.y, last.x - motionRect.x, last.y - motionRect.y);
                break;
        }
        g.drawRect(motionRect.x, motionRect.y, motionRect.width, motionRect.height);
        //cnt = getCount();
        if(!moreInput) cnt = tree.query((int)motionRect.getMinX(), (int)motionRect.getMaxX(), (int)motionRect.getMinY(), (int)motionRect.getMaxY());
        cntShower.setText(s1 + cnt);
    }

    int getCount() {
        int ret = 0;
        for(Point p : points) if(motionRect.contains(p)) ret++;
        return ret;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(!moreInput) return;
        points.add(e.getPoint());
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        frst = p;
        stx = motionRect.x;
        sty = motionRect.y;
        if (motionRect.contains(p)) {
            mode = Mode.TRANSLATE;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            Rectangle ryt = new Rectangle((int) motionRect.getMaxX(), (int) motionRect.getMinY(), 10, motionRect.height);
            if (ryt.contains(p)) {
                mode = Mode.RIGHT_EXTEND;
                setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            }
            Rectangle bottom = new Rectangle((int) motionRect.getMinX(), (int) motionRect.getMaxY(), motionRect.width, 10);
            if (bottom.contains(p)) {
                mode = Mode.BOTTOM_EXTEND;
                setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
            }
            Rectangle both = new Rectangle((int) motionRect.getMaxX(), (int) motionRect.getMaxY(), 10, 10);
            if (both.contains(p)) {
                mode = Mode.BOTH_EXTEND;
                setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mode = Mode.IDLE;
        setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    enum Mode {
        IDLE,
        TRANSLATE,
        RIGHT_EXTEND,
        BOTTOM_EXTEND,
        BOTH_EXTEND,
    }
}

class Data{
    int x, y;

    Data(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

class SegTree {
    Data[] arr;
    Node[] tree;
    //hash array for compressing y values
    int[] hash;
    int[] reHash;
    int n;
    ArrayList ar[];
    int pointCount;

    SegTree(Data[] arr, int maxR) {
        this.arr = arr;
        pointCount = arr.length;
        Arrays.sort(arr, new Comparator<Data>() {
            @Override
            public int compare(Data o1, Data o2) {
                return o1.y - o2.y;
            }
        });

        hash = new int[maxR];
        reHash = new int[pointCount];

        int hashIdx = 0;
        hash[arr[0].y] = hashIdx;
        reHash[hashIdx] = arr[0].y;
        hashIdx++;

        for (int i = 1; i < arr.length; i++) {
            if (arr[i].y == arr[i - 1].y) continue;
            hash[arr[i].y] = hashIdx;
            reHash[hashIdx] = arr[i].y;
            hashIdx++;
        }

        this.n = hashIdx;
        //System.out.println("hashIdx = " + hashIdx);

        ar = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            ar[i] = new ArrayList<Data>();
        }

        for (int i = 0; i < arr.length; i++)
            ar[hash[arr[i].y]].add(arr[i]);

        reHash = Arrays.copyOfRange(reHash, 0, hashIdx);
        //System.out.println(Arrays.toString(reHash));
        tree = new Node[4 * n];
        build(1, 0, n-1);
    }

    Node combine(Node left, Node ryt) {
        Node ret = new Node(left.sz + ryt.sz);
        int i = 0;
        int j = 0;
        int res = 0;
        while (i < left.sz && j < ryt.sz) {
            if(left.ys[i] <= ryt.ys[j]){
                ret.ys[res] = left.ys[i];
                i++;
                res++;
            } else {
                ret.ys[res] = ryt.ys[j];
                j++;
                res++;
            }
        }
        while(i < left.sz) {
            ret.ys[res] = left.ys[i];
            i++;
            res++;
        }
        while(j < ryt.sz) {
            ret.ys[res] = ryt.ys[j];
            j++;
            res++;
        }
        return ret;
    }

    void build(int v, int tl, int tr) {
        if (tl == tr) {
            tree[v] = new Node(ar[tl]);
            //System.out.println(tl + " " + tr + " " + Arrays.toString(tree[v].ys));
            //System.out.println(Arrays.toString(tree[v].mul));
            return;
        }
        int mid = tl + tr >> 1;
        build(v << 1, tl, mid);
        build(v << 1 | 1, mid + 1, tr);
        tree[v] = combine(tree[v << 1], tree[v << 1 | 1]);
        //System.out.println(tl + " " + tr + " " + Arrays.toString(tree[v].ys));
        //System.out.println(Arrays.toString(tree[v].mul));
    }

    int query(int x1, int x2, int y1, int y2) {
        //System.out.println("done " + x + " " + y);
        int li = getFirstInd(reHash, y1);
        int ri = getLastInd(reHash, y2);
        //System.out.println("li = " + li + "ri = " + ri);
        if(li == -1 || ri == -1 || li > ri) return 0;
        return query(1, 0, n-1, li, ri, x1, x2);

    }

    int query(int v, int tl, int tr, int l, int r, int fixedL, int fixedR) {
        if(tl == l && tr == r) {
            //System.out.println("tl = " + tl + " tr = " + tr + " x = " + x + " y = " + y);
            int[] arr;
            arr = tree[v].ys;
            int find = getFirstInd(arr, fixedL);
            int lind = getLastInd(arr, fixedR);
            if(find == -1 || lind == -1) return 0;
            else return lind - find + 1;
        }
        int mid = tl + tr >> 1;

        if(r <= mid) return query(v << 1, tl, mid, l, r, fixedL, fixedR);
        else if(l > mid) return query(v << 1 | 1, mid + 1, tr, l, r, fixedL, fixedR);
        else return query(v << 1, tl, mid, l, mid, fixedL, fixedR) + query(v << 1 | 1, mid + 1, tr, mid + 1, r, fixedL, fixedR);
    }

    int getFirstInd(int []arr, int ind) {
        int lo = 0, hi = arr.length - 1;
        int ans = 0;
        if(ind < arr[0]) return 0;
        if(ind > arr[arr.length - 1]) return -1;
        while(lo <= hi) {
            int mid = lo + hi >> 1;
            if(arr[mid] >= ind) {
                ans = mid;
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return ans;
    }

    int getLastInd(int []arr, int ind) {
        int lo = 0, hi = arr.length - 1;
        int ans = 0;
        if(ind < arr[0]) return -1;
        if(ind > arr[arr.length - 1]) return arr.length - 1;
        while(lo <= hi) {
            int mid = lo + hi >> 1;
            if(arr[mid] <= ind) {
                ans = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }
}

class Node {
    int[] ys;
    int sz;

    Node(ArrayList<Data> arrayList) {
        Collections.sort(arrayList, new Comparator<Data>() {
            @Override
            public int compare(Data o1, Data o2) {
                if (o1.y == o2.y) return o1.x - o2.x;
                else return o1.y - o2.y;
            }
        });
        ys = new int[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            ys[i] = arrayList.get(i).x;
        }
        sz = arrayList.size();
    }

    Node(int sz) {
        this.sz = sz;
        ys = new int[sz];
    }
}
