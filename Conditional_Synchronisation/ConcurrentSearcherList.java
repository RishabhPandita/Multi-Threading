package ITM_final;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentSearcherList<T> {

	/*
	 * Three kinds of threads share access to a singly-linked list: searchers,
	 * inserters and deleters. Searchers merely examine the list; hence they can
	 * execute concurrently with each other. Inserters add new items to the
	 * front of the list; insertions must be mutually exclusive to preclude two
	 * inserters from inserting new items at about the same time. However, one
	 * insert can proceed in parallel with any number of searches. Finally,
	 * deleters remove items from anywhere in the list. At most one deleter
	 * process can access the list at a time, and deletion must also be mutually
	 * exclusive with searches and insertions.
	 * 
	 * Make sure that there are no data races between concurrent inserters and
	 * searchers!
	 */

	private static class Node<T> {
		final T item;
		Node<T> next;

		Node(T item, Node<T> next) {
			this.item = item;
			this.next = next;
		}
	}

	private volatile Node<T> first;

	private int ni = 0; /* number of inserts */
	private int nr = 0; /* number of removes */
	private int ns = 0; /* number of search */
	private final ReentrantLock lock; /* Reentrant Lock variable */

	private final Condition insertCond; /* Condition variable used for insert */
	private final Condition removeCond; /* Condition variable used for remove */
	private final Condition searchCond; /* Condition variable used for search */

	public ConcurrentSearcherList() {
		first = null;
		lock = new ReentrantLock(); /*
									 * Initializes a new lock as soon as a new
									 * object is constructed
									 */
		insertCond = lock
				.newCondition(); /*
									 * Initializes a new condition variable as soon
									 * as a new object is constructed
									 */
		removeCond = lock.newCondition();
		searchCond = lock.newCondition();
	}

	/**
	 * Inserts the given item into the list.
	 * 
	 * Precondition: item != null
	 * 
	 * @param item
	 * @throws InterruptedException
	 */
	public void insert(T item) throws InterruptedException {
		assert item != null : "Error in ConcurrentSearcherList insert:  Attempt to insert null";
		start_insert();
		try {
			first = new Node<T>(item, first);
		} finally {
			end_insert();
		}
	}

	/**
	 * Determines whether or not the given item is in the list
	 * 
	 * Precondition: item != null
	 * 
	 * @param item
	 * @return true if item is in the list, false otherwise.
	 * @throws InterruptedException
	 */
	public boolean search(T item) throws InterruptedException {
		assert item != null : "Error in ConcurrentSearcherList insert:  Attempt to search for null";
		start_search();
		try {
			for (Node<T> curr = first; curr != null; curr = curr.next) {
				if (item.equals(curr.item))
					return true;
			}
			return false;
		} finally {
			end_search();
		}
	}

	/**
	 * Removes the given item from the list if it exists. Otherwise the list is
	 * not modified. The return value indicates whether or not the item was
	 * removed.
	 * 
	 * Precondition: item != null.
	 * 
	 * @param item
	 * @return whether or not item was removed from the list.
	 * @throws InterruptedException
	 */
	public boolean remove(T item) throws InterruptedException {
		assert item != null : "Error in ConcurrentSearcherList insert:  Attempt to remove null";
		start_remove();
		try {
			if (first == null)
				return false;
			if (item.equals(first.item)) {
				first = first.next;
				return true;
			}
			for (Node<T> curr = first; curr.next != null; curr = curr.next) {
				if (item.equals(curr.next.item)) {
					curr.next = curr.next.next;
					return true;
				}
			}
			return false;
		} finally {
			end_remove();
		}
	}

	/**
	 * We first obtain the lock and check the number of operations going on. If
	 * number of inserts are non zero or number of removes are going on then we
	 * wait till they finish up. As soon as they finish and we have are
	 * invariant satisfied we increment number of inserts and unlock as object
	 * is destructed.
	 */
	private void start_insert() throws InterruptedException {
		lock.lock();
		try {
			while (ni != 0 || nr != 0) {
				insertCond.await();
			}
			ni++;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * We first obtain the lock and decrement the number of inserts. If number
	 * of inserts hit zero we signal the remove and signal condition variables.
	 */
	private void end_insert() {
		lock.lock();
		try {
			ni--;
			if (ni == 0) {
				removeCond.signalAll();
				insertCond.signalAll();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * We first obtain the lock and check the number of operations going on. If
	 * any number of removes are going on then we wait till they finish up. As
	 * soon as they finish and we have are invariant satisfied we increment
	 * number of searches and unlock as object is destructed.
	 */
	private void start_search() throws InterruptedException {
		lock.lock();
		try {
			while (nr != 0) {
				searchCond.await();
			}
			ns++;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * We first obtain the lock and decrement the number of searches. If number
	 * of searches hit zero we signal the remove condition variables
	 */
	private void end_search() {
		lock.lock();
		try {
			ns--;
			if (ns == 0)
				removeCond.signalAll();
		} finally {
			lock.unlock();
		}

	}

	/**
	 * We first obtain the lock and check the number of operations going on. If
	 * any number of removes, searches and inserts are going on then we wait
	 * till they finish up. As soon as they finish and we have are invariant
	 * satisfied we increment number of removes and unlock as object is
	 * destructed.
	 */
	private void start_remove() throws InterruptedException {
		lock.lock();
		try {
			while (nr != 0 || ni != 0 || ns != 0) {
				removeCond.await();
			}
			nr++;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * We first obtain the lock and decrement the number of removes. If number
	 * of removes hit zero we signal the insert,search and remove condition
	 * variables
	 */
	private void end_remove() {
		lock.lock();
		try {
			nr--;
			if (nr == 0) {
				insertCond.signalAll();
				searchCond.signalAll();
				removeCond.signalAll();
			}

		} finally {
			lock.unlock();
		}
	}
	
	public static void main (String args[]) throws InterruptedException {
		ConcurrentSearcherList<Integer> c = new ConcurrentSearcherList<Integer>();
		ConcurrentSearcherList<Integer> c1 = new ConcurrentSearcherList<Integer>();
		
		c.insert(5);
		c.insert(50);
		c.insert(500);
		c1.insert(11);
		c1.remove(11);
		c.insert(5000);
		c.search(50);
		c.insert(10);
		c.remove(10);
		c1.search(11);
		c1.insert(20);
		c1.search(50);
		c1.search(20);	
		System.out.println("Testing Main Class ");

		
	}
	
}