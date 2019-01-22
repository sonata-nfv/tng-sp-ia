#!/bin/bash

# Delete VIM
/usr/bin/docker exec -t son-postgres psql -h localhost -U sonatatest -d vimregistry -c "DELETE FROM VIM WHERE UUID='1111-22222222-33333333-4444'"
/usr/bin/docker exec -t son-postgres psql -h localhost -U sonatatest -d vimregistry -c "DELETE FROM VIM WHERE UUID='aaaa-bbbbbbbb-cccccccc-dddd'"
# Delete WIM
/usr/bin/docker exec -t son-postgres psql -h localhost -U postgres -d wimregistry -c "DELETE FROM ATTACHED_VIM WHERE WIM_UUID='1234-12345678-12345678-1234'"
/usr/bin/docker exec -t son-postgres psql -h localhost -U postgres -d wimregistry -c "DELETE FROM WIM WHERE UUID='1234-12345678-12345678-1234'"