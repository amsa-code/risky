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
Each detection can be described by the tuple **(x, y, t, &delta;)** where 
* **x** and **y** are the position coordinates 
* **t** is the time of the detection (assumed 100% accurate)
* **&delta;** is the 95% error margin on the position 

The domain of values is assumed small enough that cartesian spatial distance calculation can be used instead of great-circle formulae.

So we have a set of tuples (the beacon detections for a target drifting at the ocean surface):

&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.codecogs.com/eqnedit.php?latex=\fn_jvn&space;(x_i,y_i,t_i,\delta_i)\&space;for\&space;i=1..n" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\fn_jvn&space;(x_i,y_i,t_i,\delta_i)\&space;for\&space;i=1..n" title="(x_i,y_i,t_i,\delta_i)\ for\ i=1..n" /></a>

In terms of the variance in spatial distance, we have the 95% position error margin **&delta;**. Assuming a normal distribution this suggests

&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.codecogs.com/eqnedit.php?latex=\fn_jvn&space;standard\&space;deviation\&space;\sigma&space;=&space;\frac{\delta}{3.92}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\fn_jvn&space;standard\&space;deviation\&space;\sigma&space;=&space;\frac{\delta}{3.92}" title="standard\ deviation\ \sigma = \frac{\delta}{3.92}" /></a>

therefore 

&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.codecogs.com/eqnedit.php?latex=\fn_jvn&space;variance\&space;\sigma^2&space;=&space;\frac{\delta^2}{3.92^2}" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\fn_jvn&space;variance\&space;\sigma^2&space;=&space;\frac{\delta^2}{3.92^2}" title="variance\ \sigma^2 = \frac{\delta^2}{3.92^2}" /></a>

We want to find a regression function **f** that is smooth (say with continous derivative) that takes a time as input and provides a position. This regression function will minimize (locally only perhaps) a cost function:

&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.codecogs.com/eqnedit.php?latex=\fn_jvn&space;M(f)&space;=&space;\sum_{i=1}^{n}&space;w_i&space;.&space;(f(T_i)_x&space;-&space;x_i)^2&space;&plus;&space;(f(T_i)_y&space;-&space;y_i)^2\newline&space;where\newline\newline&space;\indent&space;weight\&space;w_i&space;=&space;\frac{1}{\sigma_i^2e^{\alpha|T_i-t_i|}}\newline&space;\indent&space;variance\&space;\sigma_i^2&space;=&space;\frac{\delta_i^2}{{3.92}^2}\newline\newline&space;\indent&space;\alpha\&space;is\&space;a\&space;constant\&space;yet\&space;to\&space;be\&space;determined!" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\fn_jvn&space;M(f)&space;=&space;\sum_{i=1}^{n}&space;w_i&space;.&space;(f(T_i)_x&space;-&space;x_i)^2&space;&plus;&space;(f(T_i)_y&space;-&space;y_i)^2\newline&space;where\newline\newline&space;\indent&space;weight\&space;w_i&space;=&space;\frac{1}{\sigma_i^2e^{\alpha|T_i-t_i|}}\newline&space;\indent&space;variance\&space;\sigma_i^2&space;=&space;\frac{\delta_i^2}{{3.92}^2}\newline\newline&space;\indent&space;\alpha\&space;is\&space;a\&space;constant\&space;yet\&space;to\&space;be\&space;determined!" title="M(f) = \sum_{i=1}^{n} w_i . (f(T_i)_x - x_i)^2 + (f(T_i)_y - y_i)^2\newline where\newline\newline \indent weight\ w_i = \frac{1}{\sigma_i^2e^{\alpha|T_i-t_i|}}\newline \indent variance\ \sigma_i^2 = \frac{\delta_i^2}{{3.92}^2}\newline\newline \indent \alpha\ is\ a\ constant\ yet\ to\ be\ determined!" /></a>

The weight function was constructed by multiplying a weight due to variance (this is a standard approach) with a weight due to the difference in time (we value positions more that are closer in time to the time of interest). The exponential of the magnitude of the difference in times has been used because it has appropriate behaviour at the extremes (times close, times not close) but beyond that the use of exponential is arbitrary and further consideration will be needed to justify its use over some other function.

For example, a weight function like this might be chosen instead:

&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.codecogs.com/eqnedit.php?latex=\fn_jvn&space;weight\&space;w_i&space;=&space;\frac{1}{\sigma_i^2(\varepsilon&space;&plus;&space;s_{mean}^2(T_i-t_i)^2)}\newline\newline&space;\indent&space;where\&space;\varepsilon\&space;is\&space;some\&space;small\&space;constant\newline\newline&space;\indent\indent&space;and\&space;s_{mean}\&space;is\&space;the\&space;mean\&space;drift\&space;speed" target="_blank"><img src="https://latex.codecogs.com/gif.latex?\fn_jvn&space;weight\&space;w_i&space;=&space;\frac{1}{\sigma_i^2(\varepsilon&space;&plus;&space;s_{mean}^2(T_i-t_i)^2)}\newline\newline&space;\indent&space;where\&space;\varepsilon\&space;is\&space;some\&space;small\&space;constant\newline\newline&space;\indent\indent&space;and\&space;s_{mean}\&space;is\&space;the\&space;mean\&space;drift\&space;speed" title="weight\ w_i = \frac{1}{\sigma_i^2(\varepsilon + s_{mean}^2(T_i-t_i)^2)}\newline\newline \indent where\ \varepsilon\ is\ some\ small\ constant\newline\newline \indent\indent and\ s_{mean}\ is\ the\ mean\ drift\ speed" /></a>

A minimal solution to this problem would provide the values of f at times T<sub>1</sub>,..,T<sub>n</sub> (the times of detections). A more sophisticated solution would provide a function that could be applied to any t between T<sub>1</sub> and T<sub>n</sub>.



#Implementation
Algorithm-wise looks like the *Levenberg-Marquardt* method is used to solve the problem and *apache commons-math* is one java implementation of 
that algorithm.
