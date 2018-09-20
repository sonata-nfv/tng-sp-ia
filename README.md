[![VIM adaptor](http://jenkins.sonata-nfv.eu/buildStatus/icon?job=tng-sp-ia)](http://jenkins.sonata-nfv.eu/job/tng-sp-ia)
[![Join the chat at https://gitter.im/sonata-nfv/5gtango-sp](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/sonata-nfv/5gtango-sp)
 
 <p align="center"><img src="https://github.com/sonata-nfv/tng-api-gtw/wiki/images/sonata-5gtango-logo-500px.png" /></p>
 
# tng-sp-ia

In the TANGO Service Platform the Infrastructure Abstraction plays the role of an abstraction layer between the MANO framework and the underlying (virtualised) infrastructure.
The Infrastructure Abstraction allows the orchestrator's entities to interact with the infrastructure, regardless of the specific technology used to manage it. It exposes interfaces to manage service and VNF instances, retrieve monitoring information about the infrastructure status, reserve resources for services deployment.
It is composed of two main modules, the Infrastructure Abstractor - NorthBound Interface and the VIM/WAN wrappers.


## Repository Structure
  
 * `IA NBI` contains the NBI files.
 * `VIM/WIM Wrappers` contains the VIM/WIM Wrappers files.

## Contributing

Contributing to the son-sp-infrabstract is really easy. You must:

1. Clone [this repository](https://github.com/sonata-nfv/tng-sp-ia);
2. Work on your proposed changes, preferably through submiting [issues](https://github.com/sonata-nfv/tng-sp-ia/issues);
3. Submit a Pull Request;
4. Follow/answer related [issues](https://github.com/sonata-nfv/tng-sp-ia/issues) (see Feedback-Chanel, below).

For more information, please see the README file in the relevant subfolder.

## License

This Software is published under Apache 2.0 license. Please see the LICENSE file for more details.

## Useful Links

* https://www.openstack.org/ the OpenStack project homepage
* https://pypi.python.org/pypi/pip Python Package Index
* https://maven.apache.org/ Java Maven
* https://www.docker.com/ The Docker project
* https://docs.docker.com/compose/ Docker-compose documentation


---
#### Lead Developers

The following lead developers are responsible for this repository and have admin rights. They can, for example, merge pull requests.


#### Feedback-Channel

* You may use the mailing list [sonata-dev@lists.atosresearch.eu](mailto:sonata-dev@lists.atosresearch.eu)
* [GitHub issues](https://github.com/sonata-nfv/son-sp-infrabstract/issues)
