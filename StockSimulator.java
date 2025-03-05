import java.util.concurrent.atomic.AtomicReference;
import java.util.Random;

class Order {
    char type; // 'B' for Buy, 'S' for Sell
    int ticker;
    int quantity;
    int price;
    AtomicReference<Order> next;

    public Order(char type, int ticker, int quantity, int price) {
        this.type = type;
        this.ticker = ticker;
        this.quantity = quantity;
        this.price = price;
        this.next = new AtomicReference<>(null);
    }
}

class OrderBook {
    private AtomicReference<Order> head = new AtomicReference<>(null);

    public void addOrder(char type, int ticker, int quantity, int price) {
        Order newOrder = new Order(type, ticker, quantity, price);
        Order oldHead;
        do {
            oldHead = head.get();
            newOrder.next.set(oldHead);
        } while (!head.compareAndSet(oldHead, newOrder));
    }

    public void matchOrder() {
        Order prev = null;
        Order current = head.get();
        while (current != null && current.next.get() != null) {
            if (current.type == 'B') {
                Order temp = current.next.get();
                Order tempPrev = current;
                while (temp != null) {
                    if (temp.type == 'S' && current.ticker == temp.ticker && current.price >= temp.price) {
                        int tradeQuantity = Math.min(current.quantity, temp.quantity);
                        System.out.println("Matched: Ticker " + current.ticker + " | Price " + temp.price + " | Quantity " + tradeQuantity);

                        current.quantity -= tradeQuantity;
                        temp.quantity -= tradeQuantity;

                        if (temp.quantity == 0) {
                            tempPrev.next.set(temp.next.get());
                        }
                        if (current.quantity == 0) {
                            if (prev != null) prev.next.set(current.next.get());
                            else head.set(current.next.get());
                            break;
                        }
                    }
                    tempPrev = temp;
                    temp = temp.next.get();
                }
            }
            prev = current;
            current = current.next.get();
        }
    }
}

class StockSimulator {
    public static void simulateOrders(OrderBook book, int numOrders) {
        Random rand = new Random();
        for (int i = 0; i < numOrders; i++) {
            char type = (rand.nextInt(2) == 0) ? 'B' : 'S';
            int ticker = rand.nextInt(1024);
            int quantity = rand.nextInt(10) + 1;
            int price = rand.nextInt(100) + 1;
            book.addOrder(type, ticker, quantity, price);
        }
    }

    public static void main(String[] args) {
        OrderBook book = new OrderBook();
        Thread[] threads = new Thread[4];
        for (int i = 0; i < 4; i++) {
            threads[i] = new Thread(() -> simulateOrders(book, 25));
            threads[i].start();
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Thread matcher = new Thread(book::matchOrder);
        matcher.start();
        try {
            matcher.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}