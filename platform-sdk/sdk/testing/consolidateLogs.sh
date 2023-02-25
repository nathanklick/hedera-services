#!/usr/bin/env bash
#
# Copyright 2016-2022 Hedera Hashgraph, LLC
#
# This software is the confidential and proprietary information of
# Hedera Hashgraph, LLC. ("Confidential Information"). You shall not
# disclose such Confidential Information and shall use it only in
# accordance with the terms of the license agreement you entered into
# with Hedera Hashgraph.
#
# HEDERA HASHGRAPH MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
# THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
# TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
# PARTICULAR PURPOSE, OR NON-INFRINGEMENT. HEDERA HASHGRAPH SHALL NOT BE LIABLE FOR
# ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
# DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
#

cd "`dirname "$0"`"

oldifs="$IFS"

IFS=$'\n'
allLogFiles=( `find . -name "swirlds.log"` )

curTestName=""
for log in ${allLogFiles[@]}; do
  #echo "$log"
  IFS=$'/'
  path=( $log )
  testName="${path[$(( ${#path[@]} - 3 ))]}"
  nodeName="${path[$(( ${#path[@]} - 2 ))]}"
  
  if [[ "$curTestName" != "$testName" ]]; then
    curTestName="$testName"
    > "$testName.log"
    echo "$testName"
  fi
  
  
  echo "===================================================================================================================================" >> "$testName.log"
  echo "============================================================ $nodeName =============================================================" >> "$testName.log"
  echo "===================================================================================================================================" >> "$testName.log"
  cat "$log" >> "$testName.log"
  
  #exit
done


IFS="$oldifs"
