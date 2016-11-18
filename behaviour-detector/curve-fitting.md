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

&nbsp;&nbsp;&nbsp;&nbsp;{ (x<sub>i</sub>, y<sub>i</sub>, z<sub>i</sub>, e<sub>i</sub>) : 1 <= i <= n s.t t<sub>i+1</sub> >= t<sub>i</sub> }

We want to find a regression function **f** that is smooth (say with continous 2nd-derivative) that takes a time as input and provides a position. This regression function will minimize (locally only perhaps) a cost function:

&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.codecogs.com/eqnedit.php?latex=M&space;=&space;\sum_{i=0}^{n}&space;w_i&space;.&space;dist((f(T_i)_x,f(T_i)_y,T_i),&space;(x_i,y_i,t_i)))&space;\newline&space;where\&space;w_i&space;=&space;\frac{1}{\sigma_i^2},\newline&space;\sigma^2\&space;is\&space;the\&space;variance\&space;in\&space;distance\&space;from\&space;actual\&space;position,\newline&space;\newline&space;dist((x_1,y_1,t_1),(x_2,y_2,t_2))&space;=&space;\sqrt{(x_2-x_1)^2&space;&plus;&space;(y_2-y_1)^2&space;&plus;&space;s_{mean}^2(t_2-t_1)^2)},\newline&space;\newline&space;s_{mean}&space;is\&space;the\&space;average\&space;drift\&space;speed" target="_blank"><img src="https://latex.codecogs.com/gif.latex?M&space;=&space;\sum_{i=0}^{n}&space;w_i&space;.&space;dist((f(T_i)_x,f(T_i)_y,T_i),&space;(x_i,y_i,t_i)))&space;\newline&space;where\&space;w_i&space;=&space;\frac{1}{\sigma_i^2},\newline&space;\sigma^2\&space;is\&space;the\&space;variance\&space;in\&space;distance\&space;from\&space;actual\&space;position,\newline&space;\newline&space;dist((x_1,y_1,t_1),(x_2,y_2,t_2))&space;=&space;\sqrt{(x_2-x_1)^2&space;&plus;&space;(y_2-y_1)^2&space;&plus;&space;s_{mean}^2(t_2-t_1)^2)},\newline&space;\newline&space;s_{mean}&space;is\&space;the\&space;average\&space;drift\&space;speed" title="M = \sum_{i=0}^{n} w_i . dist((f(T_i)_x,f(T_i)_y,T_i), (x_i,y_i,t_i))) \newline where\ w_i = \frac{1}{\sigma_i^2},\newline \sigma^2\ is\ the\ variance\ in\ distance\ from\ actual\ position,\newline \newline dist((x_1,y_1,t_1),(x_2,y_2,t_2)) = \sqrt{(x_2-x_1)^2 + (y_2-y_1)^2 + s_{mean}^2(t_2-t_1)^2)},\newline \newline s_{mean} is\ the\ average\ drift\ speed" /></a>

Note that the distance function above maps time difference into distance by multiplying time by the mean drift speed. This is a somewhat arbitrary mapping but seems reasonable at the moment!

In terms of variance in the formulae above, we have the 95% position error margin **e**. Assuming a normal distribution this suggests

&nbsp;&nbsp;&nbsp;&nbsp;sd = e / 3.92

and because sd = &sqrt;variance we have 

&nbsp;&nbsp;&nbsp;&nbsp;variance = (e/3.92)<sup>2</sup>

#Implementation
Algorithm-wise looks like the *Levenberg-Marquardt* method is used to solve the problem and *apache commons-math* is one java implementation of 
that algorithm.
