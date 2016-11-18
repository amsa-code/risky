#Curve fitting for a drifting object

The arrival of MEOSAR satellites on the beacon detection scene has brought about new and unexpected difficulties for Search and
Rescue operational staff. 

Without getting into a lot of detail (I don't know it!) the issue is that beacon detections arrive for a single beacon much more frequently (say every 30 seconds
) and each report comes with a 95% error margin value (called *accuracy* in the domain). The 95% error marging of a report may range from 120m for a good quality 
GPS measurement to 15nm for when Doppler measurements are part of the calculation. Search and Rescue staff want to know the best guessed
path for the target taking into account all the detections and with so many detections of varying quality it's quite difficult to sort out.

Dave Moten's investigations so far indicate that the problem of calculating a best path (and its associated error bounds) can be described as 
a *non-linear weighted least-squares regression* problem.

Algorithm-wise looks like the *Levenberg-Marquardt* method is used to solve the problem and *apache commons-math* is one java implementation of 
that algorithm.

The problem does need precise definition though particularly as any optimization based on a distance function needs to make sense of distance across 
space *and* time (so time difference probably needs to be mapped sensibly into a spatial distance measure).

Each detection can be described by the tuple **(x, y, t, e)** where 
* **x** and **y** are the position coordinates
* **t** is the time of the detection (assumed 100% accurate)
* **e** is the 95% error margin on the position 
