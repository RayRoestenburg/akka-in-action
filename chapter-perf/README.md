Performance tests
=================

These performance test are dependent on the machine the test are run. Some test are designed to fail, due to
performance problems, as described in the book. These test are:
* aia.performance.dispatcher.DispatcherInitTest
* aia.performance.dispatcher.DispatcherPinnedTest
* aia.performance.dispatcher.DispatcherSeparateTest
* aia.performance.dispatcher.DispatcherThroughputTest

But it is possible that other tests fail too. Especially when running all the test at once.
These are designed to stress the system its running on. Therefore these test can fail on some systems
and succeed on others. These test are more to get the feeling for the effects the different configurations
can have on the performance of an application


