package zs.demo

import java.util.concurrent.ConcurrentLinkedQueue

class ExcelExecutionQueue {

	private static final ConcurrentLinkedQueue<Map> QUEUE = new ConcurrentLinkedQueue<>()

	static void add(Map execution) {
		if (execution) {
			QUEUE.add(execution)
		}
	}

	static List<Map> consumeAll() {
		List<Map> out = []

		while (true) {
			Map item = QUEUE.poll()
			if (item == null) {
				break
			}
			out << item
		}

		return out
	}

	static void clear() {
		while (QUEUE.poll() != null) {
			// limpiar cola
		}
	}
}