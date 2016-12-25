/*
 * threadLocal.h
 *  Start with this and add what is necessary
 */

#ifndef THREADLOCAL_H_
#define THREADLOCAL_H_

#include <iostream>
#include <thread>
#include <unordered_map>
#include <stdio.h>
#include <mutex>
#include <exception>

namespace cop5618 {

template <typename T>
class threadLocal {
public:
	threadLocal();
	~threadLocal(); 

	//disable copy, assign, move, and move assign constructors
	 threadLocal(const threadLocal&)=delete;
	 threadLocal& operator=(const threadLocal&)=delete;
	 threadLocal(threadLocal&&)=delete;
	 threadLocal& operator=(const threadLocal&&)=delete;

	 /**
	 * Returns the current thread's value.
	 * If no value has been previously set by this
	 * thread, an out_of_range exception is thrown.
	 */
	const T& get() const;


	/**
	 * Sets the value of the threadLocal for the current thread
	 * to val.
	 */
	void set(T val);

	/**
	 * Removes the current thread's value for the threadLocal
	 */
	void remove();

	/**
	 * Friend function.  Useful for debugging only */	 
	template <typename U>
	friend std::ostream& operator<< (std::ostream& os, const threadLocal<U>& obj);

private:
//ADD PRIVATE MEMBERS 
std::unordered_map <std::thread::id,T> thlocal;
mutable std::mutex m;
};

//ADD DEFINITOINS
template <typename U>
	std::ostream& operator<< (std::ostream& out, const threadLocal<U>& obj) {
		for (const auto &p : obj.thlocal) {
		out << "ThreadLocal[" << p.first << "] = " << p.second;
		}
	return out;
}



template <typename T>
void threadLocal <T>::remove () {

try{
		std::thread::id thread_id = std::this_thread::get_id();
		if(thlocal.find(thread_id)==thlocal.end()) {
			throw "Invalid Operation. Nothing to remove";
		}
		else {
			m.lock();
			thlocal.erase(thread_id);
			m.unlock();
		}
	}
catch(std::exception e) {
	throw "Exception thrown";
	}	
}

template <typename T>
void threadLocal <T>::set (T val) {

try {
	std::thread::id thread_id = std::this_thread::get_id();
	if(thlocal.find(thread_id)==thlocal.end()) {
		m.lock();
		thlocal[thread_id]=val;   //put in map
		m.unlock();
		std::cout<<"\nSetting "<<thlocal[thread_id]<<" for thread_id  "<<thread_id;
	}
	else {
		m.lock();
		std::cout<<"\nOverWritting: "<<thread_id;
		m.unlock();
		thlocal[thread_id]=val;  //overwrite in map
	}
	if(val != get())
		throw "\nException:Set Operation Unsuccessfull";
	}
catch (std::exception e) {
	throw "Exception Caught";
}	
}


template <typename T>
const T&  threadLocal<T>::get() const {

try {
	std::thread::id thread_id = std::this_thread::get_id();
	if(thlocal.find(thread_id) == thlocal.end()) {
			throw "Exception:Trying to get value which is not set";
	}
		std::lock_guard<std::mutex> lock(m);
		return thlocal.find(thread_id)->second;
}
catch (std::exception e) {
	throw "Exception Caught";
	}
}

template <typename T>
threadLocal <T>::threadLocal () {

}

template <typename T>
threadLocal <T>::~threadLocal () {
m.lock();
thlocal.clear();
m.unlock();
}

} /* namespace cop5618 */

#endif /* THREADLOCAL_H_ */
