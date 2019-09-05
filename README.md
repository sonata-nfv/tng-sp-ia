[![tng-sp-ia](http://jenkins.sonata-nfv.eu/buildStatus/icon?job=tng-sp-ia)](http://jenkins.sonata-nfv.eu/job/tng-sp-ia)
[![Join the chat at https://gitter.im/sonata-nfv/Lobby](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/sonata-nfv/Lobby)
 
 <p align="center"><img src="https://github.com/sonata-nfv/tng-api-gtw/wiki/images/sonata-5gtango-logo-500px.png" /></p>
 
# tng-sp-ia

In the TANGO Service Platform the Infrastructure Abstraction plays the role of an abstraction layer between the MANO framework and the underlying (virtualised) infrastructure.
The Infrastructure Abstraction allows the orchestrator's entities to interact with the infrastructure, regardless of the specific technology used to manage it. It exposes interfaces to manage service and VNF instances, retrieve monitoring information about the infrastructure status, reserve resources for services deployment.
It is composed of two main modules, the Infrastructure Abstractor - NorthBound Interface and the VIM/WAN wrappers.


## Repository Structure
  
 * `IA NBI` contains the NBI files.
 * `VIM/WIM Wrappers` contains the VIM/WIM Wrappers files.

## Installing / Getting started

TODO

## Developing

### Contributing

Contributing to the son-sp-infrabstract is really easy. You must:

1. Clone [this repository](https://github.com/sonata-nfv/tng-sp-ia);
2. Work on your proposed changes, preferably through submiting [issues](https://github.com/sonata-nfv/tng-sp-ia/issues);
3. Submit a Pull Request;
4. Follow/answer related [issues](https://github.com/sonata-nfv/tng-sp-ia/issues) (see Feedback-Chanel, below).

For more information, please see the README file in the relevant subfolder:

* [ia-nbi](https://github.com/sonata-nfv/tng-sp-ia/blob/master/ia-nbi/README.md)
* [vim-wrapper-heat](https://github.com/sonata-nfv/tng-sp-ia/blob/master/vim-wrapper-heat/README.md)
* [vim-wrapper-mock](https://github.com/sonata-nfv/tng-sp-ia/blob/master/vim-wrapper-mock/README.md)
* [vim-wrapper-ovs](https://github.com/sonata-nfv/tng-sp-ia/blob/master/vim-wrapper-ovs/README.md)
* [wim-wrapper-mock](https://github.com/sonata-nfv/tng-sp-ia/blob/master/wim-wrapper-mock/README.md)
* [wim-wrapper-vtn](https://github.com/sonata-nfv/tng-sp-ia/blob/master/wim-wrapper-vtn/README.md)


## Versioning

The most up-to-date version is v5. For the versions available, see the [link to tags on this repository](https://github.com/sonata-nfv/tng-sp-ia/releases).


## Configuration

TODO

## Tests

TODO

## Style guide

TODO

## Api Reference

We have specified the REST API in a [swagger](https://github.com/sonata-nfv/tng-sp-ia/blob/master/doc/swagger.json)-formated file and in the [wiki](https://github.com/sonata-nfv/tng-sp-ia/wiki/Rest-API-Reference).

We have specified the RabbitMQ API in the [wiki](https://github.com/sonata-nfv/tng-sp-ia/wiki/RabbitMQ-API-Reference).

## Database

TODO

## License

This Software is published under Apache 2.0 license. Please see the LICENSE file for more details.

#### Lead Developers

The following lead developers are responsible for this repository and have admin rights. They can, for example, merge pull requests.
* Carlos Marques (@carlos-martins-marques)

#### Feedback-Channel

* You may use the mailing list [sonata-dev@lists.atosresearch.eu](mailto:sonata-dev@lists.atosresearch.eu)
* [GitHub issues](https://github.com/sonata-nfv/tng-sp-ia/issues)
