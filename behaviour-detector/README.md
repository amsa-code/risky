behaviour-detector
====================


Drift detection
------------------
Using course, heading and speed we have a simple criterion for detecting if a position report is a drift candidate. Complexity hits when we want to answer this question:

* When did the vessel **start** drifting?

To answer this question the following algorithm is proposed:

Define `E` as the maximum time between two drift detections for them to be considered as one drift path.

Define `T` as the maximum time that a vessel can stop drifting before breaking a drift path.

Before recording a drift path we require at least two drift detections on the same path.

Now let's introduce some notation that will make the algorithm much more concise to explain.

`D` is a drift detection, `N` is a non drifting report.

We now want to process a stream of position reports (any reports out of time order are chucked). Consider a small buffer that we pass reports through and sometimes drift paths come out the other side. Using the rules below we will see that the buffer only needs to hold a maximum of three elements at a time.

This is how we represent a sequence of reports and how we reference time for the reports:

  D<sub>1</sub>N<sub>2</sub> represents a drift detection at time t<sub>1</sub> followed by a non drift detection at time t<sub>2</sub>.

