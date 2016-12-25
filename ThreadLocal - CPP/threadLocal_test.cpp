
#include "threadLocal.h"
#include <iostream>
#include <atomic>
#include <thread>
#include <mutex>
#include <vector>
#include <algorithm>
#include <exception>
#include <list>

int error_count;
std::mutex m;
cop5618::threadLocal <int> t1; 
cop5618::threadLocal <int> t2; 
cop5618::threadLocal <int> t3; 
cop5618::threadLocal <int> t4; 
cop5618::threadLocal <int> t5; 


/*************************************************/
/* TEST CASE 1 EXPLAINED :
 * Single thread for just checking the basic function operatorations
 * Set a thread local variable
 * get that variable
 * remove that variable
 * get it again 
 * RESULT: It should throw an exception then test case passed
 * */
 
void test_aa(int val) {

	int temp;
	std::thread::id thread_id = std::this_thread::get_id();

	try {
		t1.set(val); //set a value on t1
		temp=t1.get();    //get a value of t1
		std::cout<<"\nGetting  "<<temp<<" for thread_id  "<<thread_id;
		std::cout<<std::endl<<"Value: "<<t1.get()<<" removed for thread_id "<<thread_id;;
		t1.remove();    //remove value of t1
		temp=t1.get();    //get a value that is removed for t1
		std::cout<<std::endl<<"Faulty get succeeded :Value received "<<temp;
		m.lock();
		error_count++;
		m.unlock();
	}
	catch(const char* msg) {
		std::cerr<<std::endl<<msg;
	}

}
void test_thread1() {
	
	std::cout<<"\n\nTest Case 1 : Single thread SET,GET, REMOVE, GET";
	std::thread th1(test_aa,61);
	th1.join();
}

/*************************************************/
/* TEST CASE 2 EXPLAINED
 * get value of not-set variable
 * remove that variable
 * RESULT: It should throw an exception for get then Test case passed
 * */
void test_ab(int val) {

	int temp;
	try {
		temp=t2.get();    //get a non existent value
		std::cout<<std::endl<<"Faulty get operation: value received "<<temp;
		t2.remove();    //remove value of t2
		std::cout<<std::endl<<"Faulty remove operation :value removed from map ";
		
		m.lock();
		error_count++;
		m.unlock();
	}
	catch(const char* msg) {
		std::cerr<<std::endl<<msg;
	}
}

void test_thread2() {
	std::cout<<"\n\nTest Case 2 Single thread GET, REMOVE";
	std::thread th1(test_ab,7);
	th1.join();
}
/*************************************************/

/* TEST CASE 3 EXPLAINED
 * SET t1 value by two different threads
 * get t1 value for each thread
 * 
 * */
void test_ac(int val) {
	
	try {
		t1.set(val); //set a value on t1
		std::this_thread::sleep_for(std::chrono::milliseconds(1000));  //sleep because we want to be sure that it has its own value
		int temp=t1.get(); //get a value on t1
		std::thread::id thread_id = std::this_thread::get_id();
		std::cout<<"\nGetting  "<<temp<<" for thread_id  "<<thread_id;
	}
	catch(const char* msg) {
		std::cerr<<std::endl<<msg;
		m.lock();
		error_count++;
		m.unlock();
	}
}
void test_ac1(int val) {
	
	try {
		t1.set(val); //set a value on t1
		int temp=t1.get(); //get a value on t1
		std::thread::id thread_id = std::this_thread::get_id();
		std::cout<<"\nGetting  "<<temp<<" for thread_id  "<<thread_id;
		
	}
	catch(const char* msg) {
		std::cerr<<std::endl<<msg;
		m.lock();
		error_count++;
		m.unlock();
	}
}

void test_thread3() {
	std::cout<<"\n\nTest Case 3 MultiThread Operation SET,SLEEP,SET,GET,GET";
	std::thread th1(test_ac,7);
	std::thread th2(test_ac1,16);
	th1.join();
	th2.join();
}
/*************************************************/

/**
 * TEST CASE 4 Explained
 * Set non existing value to variable : thread1.
 * Remove the non existing value of variable by thread2.
 * Call get of thread1 to check thread2 did remove its thread1's variable.
 * RESULT: Thread 2 should throw an exception.
 **/
void test_ad(int val) {
	try{
		t2.set(val);
		std::this_thread::sleep_for(std::chrono::milliseconds(100));
		std::cout<<"\nThread1 Value after calling remove of thread2 ";
		int temp = t2.get();
		std::thread::id thread_id = std::this_thread::get_id();
		std::cout<<"\nGetting  "<<temp<<" for thread_id  "<<thread_id;
	}
	catch(const char* msg) {
		std::cerr<<std::endl<<msg;
		m.lock();
		error_count++;
		m.unlock();
	}
}
void test_ad1(int val) {
	try{
		t2.remove();
		std::thread::id thread_id = std::this_thread::get_id();
		std::cout<<"\nFaulty Remove : thread_id "<<thread_id;
		m.lock();
		error_count++;
		m.unlock();
	}
	catch(const char* msg) {
		std::cerr<<std::endl<<msg;
	}
}

void test_thread4() {
	std::cout<<"\n\nTest Case 4 MultiThread Operation SET SLEEP REMOVE GET";
	std::thread th1(test_ad,7);
	std::thread th2(test_ad1,12);
	th1.join();
	th2.join();
}
/*************************************************/

/**
 * TEST CASE 5 Explained
 * Set non existing value for variable: thread1.
 * Set non existing value to variable: thread2.
 * Remove the value of variable by thread2.
 * Call get of thread1 to check thread2 did remove thread1's variable.
 * RESULT: No exception should be thrown
 **/
 
void test_ae(int val) {
	try{
		t3.set(val);
		std::this_thread::sleep_for(std::chrono::milliseconds(1000));
		std::cout<<"\nThread1 Value after calling remove of thread2 ";
		int temp = t3.get();
		std::thread::id thread_id = std::this_thread::get_id();
		std::cout<<"\nGetting  "<<temp<<" for thread_id  "<<thread_id;
	}
	catch(const char* msg) {
		std::cerr<<std::endl<<msg;
		m.lock();
		error_count++;
		m.unlock();
	}
}
void test_ae1(int val) {
	try{
		std::thread::id thread_id = std::this_thread::get_id();
		t3.set(val);
		t3.remove();
		std::cout<<"\nRemoved thread2 local storage "<<thread_id;
	}
	catch(const char* msg) {
		std::cerr<<std::endl<<msg;
		m.lock();
		error_count++;
		m.unlock();
		
	}
}

void test_thread5() {
	std::cout<<"\n\nTest Case 5 MultiThread Operation SET SLEEP SET REMOVE GET";
	std::thread th1(test_ae,91);
	std::thread th2(test_ae1,92);
	th1.join();
	th2.join();
	
}
/***************************************************/

/**
 * TEST CASE 6 Explained SET SLEEP SET REMOVE REMOVE
 * Set non existing value for variable: thread1.
 * Remove value from thread1
 * Set non existing value to variable: thread2.
 * remove thread from thread2
 * RESULT: No exception should be thrown
 **/
 
void test_ee(int val) {
	try{
		std::thread::id thread_id = std::this_thread::get_id();
		t4.set(val);
		std::this_thread::sleep_for(std::chrono::milliseconds(1000));
		t4.remove();
		std::cout<<"\nRemoved thread1 local storage "<<thread_id;
	}
	catch(const char* msg) {
		std::cerr<<std::endl<<msg;
		m.lock();
		error_count++;
		m.unlock();
	}
}
void test_ee1(int val) {
	try{
		std::thread::id thread_id = std::this_thread::get_id();
		t4.set(val);
		t4.remove();
		std::cout<<"\nRemoved thread2 local storage "<<thread_id;
	}
	catch(const char* msg) {
		std::cerr<<std::endl<<msg;
		m.lock();
		error_count++;
		m.unlock();
	}
}

void test_thread6() {
	std::cout<<"\n\nTest Case 6 MultiThread Operation SET SLEEP SET REMOVE REMOVE";
	std::thread th1(test_ee,21);
	std::thread th2(test_ee1,22);
	th1.join();
	th2.join();
}
/********************************************************/

/**
 * TEST CASE 7 Explained REMOVE REMOVE
  * Remove non existing value from thread1
  * Remove non existing value from thread2
 *  RESULT: Exception should be thrown
 **/
 
void test_ff(char val) {
	try{
		t4.remove();
		std::thread::id thread_id = std::this_thread::get_id();
		std::cout<<"\nFaulty Remove: thread_id: "<<thread_id;
		m.lock();
		error_count++;
		m.unlock();
	}
	catch(const char* msg) {
		std::cerr<<std::endl<<msg;
	}
}
void test_thread7() {
	std::cout<<"\n\nTest Case 7 MultiThread Operation REMOVE REMOVE";
	std::thread th1(test_ff,'A');
	std::thread th2(test_ff,'B');
	th1.join();
	th2.join();
}



int test_threadLocal() {

	test_thread1();
	test_thread2();	
	test_thread3();
	test_thread4();
	test_thread5();
	test_thread6();
	test_thread7(); 
	return error_count;
}
