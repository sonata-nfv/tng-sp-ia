pipeline {
  agent any
  stages {
    stage('Unittests'){
      when{
        branch 'master'
      } 
      parallel {
        stage('Unittest IA nbi') {
          steps {
            sh './pipeline/unittest/ia-nbi.sh'
          }
        }
        stage('Unittest VIM wrapper heat') {
          steps {
            sh './pipeline/unittest/vim-wrapper-heat.sh'
          }
        }
        stage('Unittest VIM wrapper mock') {
          steps {
            sh './pipeline/unittest/vim-wrapper-mock.sh'
          }
        }
        stage('Unittest VIM wrapper ovs') {
          steps {
            sh './pipeline/unittest/vim-wrapper-ovs.sh'
          }
        }
        stage('Unittest WIM wrapper mock') {
          steps {
            sh './pipeline/unittest/wim-wrapper-mock.sh'
          }
        }
        stage('Unittest WIM wrapper vtn') {
          steps {
            sh './pipeline/unittest/wim-wrapper-vtn.sh'
          }
        }
      }
    }
    stage('Build') {
      parallel {
        stage('IA nbi') {
          steps {
            echo 'Building IA nbi container'
            sh './pipeline/build/ia-nbi.sh'
          }
        }
        stage('VIM wrapper heat') {
          steps {
            echo 'Building VIM wrapper heat container'
            sh './pipeline/build/vim-wrapper-heat.sh'
          }
        }
        stage('VIM wrapper mock') {
          steps {
            echo 'Building VIM wrapper mock container'
            sh './pipeline/build/vim-wrapper-mock.sh'
          }
        }
        stage('VIM wrapper ovs') {
          steps {
            echo 'Building VIM wrapper ovs container'
            sh './pipeline/build/vim-wrapper-ovs.sh'
          }
        }
        stage('WIM wrapper mock') {
          steps {
            echo 'Building WIM wrapper mock container'
            sh './pipeline/build/wim-wrapper-mock.sh'
          }
        }
        stage('WIM wrapper vtn') {
          steps {
            echo 'Building WIM wrapper vtn container'
            sh './pipeline/build/wim-wrapper-vtn.sh'
          }
        }
      }
    }
    stage('Publish to :latest') {
      parallel {
        stage('IA nbi') {
          steps {
            echo 'Publishing IA nbi container'
            sh './pipeline/publish/ia-nbi.sh latest'
          }
        }
        stage('VIM wrapper heat') {
          steps {
            echo 'Publishing VIM wrapper heat container'
            sh './pipeline/publish/vim-wrapper-heat.sh latest'
          }
        }
        stage('VIM wrapper mock') {
          steps {
            echo 'Publishing VIM wrapper mock container'
            sh './pipeline/publish/vim-wrapper-mock.sh latest'
          }
        }
        stage('VIM wrapper ovs') {
          steps {
            echo 'Publishing VIM wrapper ovs container'
            sh './pipeline/publish/vim-wrapper-ovs.sh latest'
          }
        }
        stage('WIM wrapper mock') {
          steps {
            echo 'Publishing WIM wrapper mock container'
            sh './pipeline/publish/wim-wrapper-mock.sh latest'
          }
        }
        stage('WIM wrapper vtn') {
          steps {
            echo 'Publishing WIM wrapper vtn container'
            sh './pipeline/publish/wim-wrapper-vtn.sh latest'
          }
        }
      }
    }
    stage('Deploying in pre-integration ') {
      when{
        not{
          branch 'master'
        }        
      }      
      steps {
        sh 'rm -rf tng-devops || true'
        sh 'git clone https://github.com/sonata-nfv/tng-devops.git'
        dir(path: 'tng-devops') {
          sh 'ansible-playbook roles/sp.yml -i environments -e "target=pre-int-sp component=infrastructure-abstraction"'
        }
      }
    }
    stage('Publishing to :int') {
      when{
        branch 'master'
      }      
      parallel {
        stage('IA nbi') {
          steps {
            echo 'Publishing IA nbi container'
            sh './pipeline/publish/ia-nbi.sh int'
          }
        }
        stage('VIM wrapper heat') {
          steps {
            echo 'Publishing VIM wrapper heat container'
            sh './pipeline/publish/vim-wrapper-heat.sh int'
          }
        }
        stage('VIM wrapper mock') {
          steps {
            echo 'Publishing VIM wrapper mock container'
            sh './pipeline/publish/vim-wrapper-mock.sh int'
          }
        }
        stage('VIM wrapper ovs') {
          steps {
            echo 'Publishing VIM wrapper ovs container'
            sh './pipeline/publish/vim-wrapper-ovs.sh int'
          }
        }
        stage('WIM wrapper mock') {
          steps {
            echo 'Publishing WIM wrapper mock container'
            sh './pipeline/publish/wim-wrapper-mock.sh int'
          }
        }
        stage('WIM wrapper vtn') {
          steps {
            echo 'Publishing WIM wrapper vtn container'
            sh './pipeline/publish/wim-wrapper-vtn.sh int'
          }
        }
      }
    }
    stage('Deploying in integration') {
      when{
        branch 'master'
      }      
      steps {
        sh 'rm -rf tng-devops || true'
        sh 'git clone https://github.com/sonata-nfv/tng-devops.git'
        dir(path: 'tng-devops') {
          sh 'ansible-playbook roles/sp.yml -i environments -e "target=int-sp component=infrastructure-abstraction"'
        }
      }
    }

    stage('Promoting release v5.0') {
      when {
        branch 'v5.0'
      }
      stages {
        stage('Generating release') {
          steps {
            sh 'docker tag registry.sonata-nfv.eu:5000/ia-nbi:latest registry.sonata-nfv.eu:5000/ia-nbi:v5.0'
            sh 'docker tag registry.sonata-nfv.eu:5000/ia-nbi:latest sonatanfv/ia-nbi:v5.0'
            sh 'docker push registry.sonata-nfv.eu:5000/ia-nbi:v5.0'
            sh 'docker push sonatanfv/ia-nbi:v5.0'
            sh 'docker tag registry.sonata-nfv.eu:5000/vim-wrapper-heat:latest registry.sonata-nfv.eu:5000/vim-wrapper-heat:v5.0'
            sh 'docker tag registry.sonata-nfv.eu:5000/vim-wrapper-heat:latest sonatanfv/vim-wrapper-heat:v5.0'
            sh 'docker push registry.sonata-nfv.eu:5000/vim-wrapper-heat:v5.0'
            sh 'docker push sonatanfv/vim-wrapper-heat:v5.0'
            sh 'docker tag registry.sonata-nfv.eu:5000/vim-wrapper-mock:latest registry.sonata-nfv.eu:5000/vim-wrapper-mock:v5.0'
            sh 'docker tag registry.sonata-nfv.eu:5000/vim-wrapper-mock:latest sonatanfv/vim-wrapper-mock:v5.0'
            sh 'docker push registry.sonata-nfv.eu:5000/vim-wrapper-mock:v5.0'
            sh 'docker push sonatanfv/vim-wrapper-mock:v5.0'
            sh 'docker tag registry.sonata-nfv.eu:5000/vim-wrapper-ovs:latest registry.sonata-nfv.eu:5000/vim-wrapper-ovs:v5.0'
            sh 'docker tag registry.sonata-nfv.eu:5000/vim-wrapper-ovs:latest sonatanfv/vim-wrapper-ovs:v5.0'
            sh 'docker push registry.sonata-nfv.eu:5000/vim-wrapper-ovs:v5.0'
            sh 'docker push sonatanfv/vim-wrapper-ovs:v5.0'
            sh 'docker tag registry.sonata-nfv.eu:5000/wim-wrapper-mock:latest registry.sonata-nfv.eu:5000/wim-wrapper-mock:v5.0'
            sh 'docker tag registry.sonata-nfv.eu:5000/wim-wrapper-mock:latest sonatanfv/wim-wrapper-mock:v5.0'
            sh 'docker push registry.sonata-nfv.eu:5000/wim-wrapper-mock:v5.0'
            sh 'docker push sonatanfv/wim-wrapper-mock:v5.0'
            sh 'docker tag registry.sonata-nfv.eu:5000/wim-wrapper-vtn:latest registry.sonata-nfv.eu:5000/wim-wrapper-vtn:v5.0'
            sh 'docker tag registry.sonata-nfv.eu:5000/wim-wrapper-vtn:latest sonatanfv/wim-wrapper-vtn:v5.0'
            sh 'docker push registry.sonata-nfv.eu:5000/wim-wrapper-vtn:v5.0'
            sh 'docker push sonatanfv/wim-wrapper-vtn:v5.0'
          }
        }
        stage('Deploying in v5.0 servers') {
          steps {
            sh 'rm -rf tng-devops || true'
            sh 'git clone https://github.com/sonata-nfv/tng-devops.git'
            dir(path: 'tng-devops') {
              sh 'ansible-playbook roles/sp.yml -i environments -e "target=sta-sp-v5-0 component=infrastructure-abstraction"'
            }
          }
        }
      }
    }


  }
  post {
    always {
      echo 'Clean Up'
    }
    success {
        emailext (
          subject: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
          body: """<p>SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
            <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
        recipientProviders: [[$class: 'DevelopersRecipientProvider']]
        )
      }
    failure {
      emailext (
          subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
          body: """<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
            <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
          recipientProviders: [[$class: 'DevelopersRecipientProvider']]
        )
    }  
  }
}
