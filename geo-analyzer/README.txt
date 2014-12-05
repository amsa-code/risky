Reasons to open-ssource this project with an Apache 2 licence (do what you like with it):
------------------------------------------------------------------------------------------

* Academic partners can view code and documentation and question assumptions or suggest enhancements
* Partners can download and run project on their test data sets without dependencies on AMSA infrastructure (vpn) or AMSA dependencies (database or internal code base)
* A well documented project will attract contributions including enhancements and bug reports from the programming community
* Hosting source code externally facilitates collaboration with external contractors and remote development without a VPN by AMSA staff.

Reasons not to open source
----------------------------

* We could monetize the program. Not a powerful argument as it would take quite some marketing to find purchasers. Many potential clients would probably just write their own and it would be preferrable if those clients simply contributed to our open source project.
* It would expose security risks for AMSA systems. Nope, its not that sort of code, it has nothing to do with public facing web systems for example.
* Source code will be harder to manage because it is outside of our single tree java code base in AMSA. Yep this is true but the code is sufficiently stand alone in nature and separated from core ER business that this will not be a big problem. The one overlap is the great circle navigation utilities class that would be copied over into the project if open-sourced.

Proposal 
----------------
Add the project to amsa-code account on github (http://github.com/amsa-code).
