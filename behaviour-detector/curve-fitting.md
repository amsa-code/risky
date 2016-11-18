#Curve fitting for a drifting object

The arrival of MEOSAR satellites on the beacon detection scene has brought about new and unexpected difficulties for Search and
Rescue operational staff. 

Without getting into a lot of detail (I don't know it!) the issue is that beacon detections arrive for a single beacon much more frequently (say every 30 seconds
) and each report comes with a 95% error margin value (called *accuracy* in the domain). The 95% error marging of a report may range from 120m for a good quality 
GPS measurement to 15nm for when Doppler measurements are part of the calculation. Search and Rescue staff want to know the best guessed
path for the target taking into account all the detections and with so many detections of varying quality it's quite difficult to sort out.

Dave Moten's investigations so far indicate that one viable approach to calculating a best path (and its associated error bounds) is by applying a technique called *non-linear weighted least-squares regression*.

The problem does need precise definition though particularly as any optimization based on a distance function needs to make sense of distance across space *and* time (so time difference probably needs to be mapped sensibly into a spatial distance measure).

##Definition
Each detection can be described by the tuple **(x, y, t, e)** where 
* **x** and **y** are the position coordinates 
* **t** is the time of the detection (assumed 100% accurate)
* **e** is the 95% error margin on the position 

The domain of values is assumed small enough that cartesian spatial distance calculation can be used instead of great-circle formulae.

So we have a set of tuples (the beacon detections for a target drifting at the ocean surface):

&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.codecogs.com/eqnedit.php?latex=\fn_jvn&space;(x_i,y_i,t_i,\delta_i)\&space;for\&space;i=1..n" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\fn_jvn&space;(x_i,y_i,t_i,\delta_i)\&space;for\&space;i=1..n" title="(x_i,y_i,t_i,\delta_i)\ for\ i=1..n" /></a>

In terms of the variance in spatial distance, we have the 95% position error margin **e**. Assuming a normal distribution this suggests

&nbsp;&nbsp;&nbsp;&nbsp;sd = e / 3.92

and because sd = &radic;variance we have 

&nbsp;&nbsp;&nbsp;&nbsp;variance = (e/3.92)<sup>2</sup>

We want to find a regression function **f** that is smooth (say with continous derivative) that takes a time as input and provides a position. This regression function will minimize (locally only perhaps) a cost function:

&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.codecogs.com/eqnedit.php?latex=\fn_jvn&space;M(f)&space;=&space;\sum_{i=1}^{n}&space;w_i&space;.&space;(f(T_i)_x&space;-&space;x_i)^2&space;&plus;&space;(f(T_i)_y&space;-&space;y_i)^2\newline&space;where\newline\newline&space;\indent&space;weight\&space;w_i&space;=&space;\frac{1}{\sigma_i^2e^{\alpha|T_i-t_i|}}\newline&space;\indent&space;variance\&space;\sigma_i^2&space;=&space;\frac{e_i^2}{{3.92}^2}\newline\newline&space;\indent&space;\alpha\&space;is\&space;a\&space;constant\&space;yet\&space;to\&space;be\&space;determined!" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\fn_jvn&space;M(f)&space;=&space;\sum_{i=1}^{n}&space;w_i&space;.&space;(f(T_i)_x&space;-&space;x_i)^2&space;&plus;&space;(f(T_i)_y&space;-&space;y_i)^2\newline&space;where\newline\newline&space;\indent&space;weight\&space;w_i&space;=&space;\frac{1}{\sigma_i^2e^{\alpha|T_i-t_i|}}\newline&space;\indent&space;variance\&space;\sigma_i^2&space;=&space;\frac{e_i^2}{{3.92}^2}\newline\newline&space;\indent&space;\alpha\&space;is\&space;a\&space;constant\&space;yet\&space;to\&space;be\&space;determined!" title="M(f) = \sum_{i=1}^{n} w_i . (f(T_i)_x - x_i)^2 + (f(T_i)_y - y_i)^2\newline where\newline\newline \indent weight\ w_i = \frac{1}{\sigma_i^2e^{\alpha|T_i-t_i|}}\newline \indent variance\ \sigma_i^2 = \frac{e_i^2}{{3.92}^2}\newline\newline \indent \alpha\ is\ a\ constant\ yet\ to\ be\ determined!" /></a>

Note that the distance function above maps time difference into distance by multiplying time by the mean drift speed. This is a somewhat arbitrary mapping but seems reasonable at the moment!

A minimal solution to this problem would provide the values of f at times T<sub>1</sub>,..,T<sub>n</sub> (the times of detections). A more sophisticated solution would provide a function that could be applied to any t between T<sub>1</sub> and T<sub>n</sub>.



#Implementation
Algorithm-wise looks like the *Levenberg-Marquardt* method is used to solve the problem and *apache commons-math* is one java implementation of 
that algorithm.
