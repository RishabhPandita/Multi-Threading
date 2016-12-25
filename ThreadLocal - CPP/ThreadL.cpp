/** Main function for threadl
 *
 * Make sure your program runs with this.
 */
 
#include<iostream>
#include "threadLocal_test.cpp"
int test_threadLocal();

int main(){
	int err = test_threadLocal();
	if(err == 0)
		std::cout<<"\nAll Test Cases Passed";

	return err;
}
